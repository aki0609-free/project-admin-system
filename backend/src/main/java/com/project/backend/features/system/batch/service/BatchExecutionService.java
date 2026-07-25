package com.project.backend.features.system.batch.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.batch.context.BatchJobExecutionContext;
import com.project.backend.features.system.batch.dto.BatchExecuteResponse;
import com.project.backend.features.system.batch.dto.BatchExecutionLogResponse;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.dto.BatchJobRunResult;
import com.project.backend.features.system.batch.entity.BatchExecutionLog;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.enums.BatchExecutionTrigger;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;
import com.project.backend.features.system.batch.service.builder.BatchExecutionResponseBuilder;
import com.project.backend.features.system.batch.service.executor.BatchJobExecutorResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchExecutionService {

    private static final Duration EXECUTION_LOCK_TTL = Duration.ofMinutes(30);

    private final BatchJobDefinitionLookupService definitionLookupService;
    private final BatchJobDefinitionRepository definitionRepository;
    private final BatchExecutionLogService logService;
    private final BatchJobExecutorResolver executorResolver;
    private final BatchExecutionResponseBuilder responseBuilder;
    private final BatchExecutionParameterService parameterService;
    private final RedisBatchJobLockService lockService;
    private final Clock clock;

    public BatchExecuteResponse executeNow(String jobCode) {
        return executeNow(jobCode, Map.of());
    }

    public BatchExecuteResponse executeNow(
            String jobCode,
            Map<String, Object> params
    ) {
        BatchJobDefinition definition =
                definitionLookupService.findActiveByJobCode(jobCode);

        if (!definition.isImmediateExecutable()) {
            throw new RuntimeException(
                    "このバッチは即時実行できません。 jobCode=" + jobCode
            );
        }

        return executeInternal(
                definition,
                parameterService.validateAndNormalize(params),
                BatchExecutionTrigger.MANUAL,
                null
        );
    }

    public BatchExecuteResponse executeScheduled(String jobCode) {
        BatchJobDefinition definition =
                definitionLookupService.findActiveByJobCode(jobCode);

        if (!definition.isScheduleEnabled()) {
            throw new RuntimeException(
                    "このバッチはスケジュール実行できません。 jobCode=" + jobCode
            );
        }

        return executeInternal(
                definition,
                Map.of(),
                BatchExecutionTrigger.SCHEDULED,
                null
        );
    }

    public BatchExecuteResponse retry(Long logId) {
        BatchExecutionLog sourceLog = logService.findRetryTarget(logId);
        BatchJobDefinition definition =
                definitionLookupService.findActiveByJobCode(sourceLog.getJobCode());

        return executeInternal(
                definition,
                logService.restoreParameters(sourceLog),
                BatchExecutionTrigger.RETRY,
                sourceLog.getId()
        );
    }

    public List<BatchExecutionLogResponse> findLogs() {
        return logService.findLogs();
    }

    public List<BatchExecutionLogResponse> findLogsByJobCode(String jobCode) {
        return logService.findLogsByJobCode(jobCode);
    }

    public BatchJobRunResult executeNowForResult(
            String jobCode,
            Map<String, Object> params
    ) {
        BatchJobDefinition definition =
                definitionLookupService.findActiveByJobCode(jobCode);

        if (!definition.isImmediateExecutable()) {
            throw new RuntimeException(
                    "このバッチは即時実行できません。 jobCode=" + jobCode
            );
        }

        Map<String, Object> normalizedParams =
                parameterService.validateAndNormalize(params);
        String lockKey = lockKey(definition);
        String lockValue = acquireLock(lockKey, definition.getJobCode());

        try {
            BatchExecutionLog executionLog = logService.start(
                    definition,
                    BatchExecutionTrigger.MANUAL,
                    normalizedParams,
                    null
            );

            try {
                BatchJobExecutionResult result = executeJob(
                        definition,
                        executionLog,
                        normalizedParams
                );
                Instant finishedAt = Instant.now(clock);

                logService.complete(executionLog, result, finishedAt);
                updateLastExecutedAt(definition, finishedAt);

                return BatchJobRunResult.builder()
                        .executionLogId(executionLog.getId())
                        .result(result)
                        .build();

            } catch (Exception e) {
                logService.fail(executionLog, e);
                updateLastExecutedAt(definition, Instant.now(clock));
                throw e;
            }
        } finally {
            lockService.unlock(lockKey, lockValue);
        }
    }

    private BatchExecuteResponse executeInternal(
            BatchJobDefinition definition,
            Map<String, Object> params,
            BatchExecutionTrigger triggerType,
            Long retrySourceLogId
    ) {
        String lockKey = lockKey(definition);
        String lockValue = acquireLock(lockKey, definition.getJobCode());

        try {
            BatchExecutionLog executionLog = logService.start(
                    definition,
                    triggerType,
                    params,
                    retrySourceLogId
            );

            try {
                BatchJobExecutionResult result =
                        executeJob(definition, executionLog, params);
                Instant finishedAt = Instant.now(clock);

                logService.complete(executionLog, result, finishedAt);
                updateLastExecutedAt(definition, finishedAt);

                return responseBuilder.completed(
                        definition,
                        executionLog,
                        result
                );

            } catch (Exception e) {
                log.error(
                        "Batch execution failed. jobCode={}, jobType={}, targetCode={}",
                        definition.getJobCode(),
                        definition.getJobType(),
                        definition.getTargetCode(),
                        e
                );

                logService.fail(executionLog, e);
                updateLastExecutedAt(definition, Instant.now(clock));

                return responseBuilder.failed(definition, executionLog);
            }
        } finally {
            lockService.unlock(lockKey, lockValue);
        }
    }

    private BatchJobExecutionResult executeJob(
            BatchJobDefinition definition,
            BatchExecutionLog executionLog,
            Map<String, Object> params
    ) {
        BatchJobExecutionContext context = new BatchJobExecutionContext(
                definition,
                executionLog,
                params
        );

        return executorResolver
                .resolve(definition.getJobType())
                .execute(context);
    }

    private String acquireLock(String lockKey, String jobCode) {
        String lockValue = lockService.tryLock(lockKey, EXECUTION_LOCK_TTL);
        if (lockValue == null) {
            throw new RuntimeException(
                    "同じバッチが実行中です。完了後に再実行してください。 jobCode=" + jobCode
            );
        }
        return lockValue;
    }

    private String lockKey(BatchJobDefinition definition) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = definition.getTenantId();
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new RuntimeException("テナント情報を取得できません。");
        }
        return "batch:execution:" + tenantId + ":" + definition.getJobCode();
    }

    private void updateLastExecutedAt(
            BatchJobDefinition definition,
            Instant executedAt
    ) {
        definition.setLastExecutedAt(executedAt);
        definitionRepository.save(definition);
    }
}
