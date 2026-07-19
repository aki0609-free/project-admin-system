package com.project.backend.features.system.batch.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.batch.context.BatchJobExecutionContext;
import com.project.backend.features.system.batch.dto.BatchExecuteResponse;
import com.project.backend.features.system.batch.dto.BatchExecutionLogResponse;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.dto.BatchJobRunResult;
import com.project.backend.features.system.batch.entity.BatchExecutionLog;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;
import com.project.backend.features.system.batch.service.builder.BatchExecutionResponseBuilder;
import com.project.backend.features.system.batch.service.executor.BatchJobExecutorResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchExecutionService {

        private final BatchJobDefinitionLookupService definitionLookupService;
        private final BatchJobDefinitionRepository definitionRepository;
        private final BatchExecutionLogService logService;
        private final BatchJobExecutorResolver executorResolver;
        private final BatchExecutionResponseBuilder responseBuilder;

        public BatchExecuteResponse executeNow(String jobCode) {
                return executeNow(jobCode, Map.of());
        }

        public BatchExecuteResponse executeNow(
                        String jobCode,
                        Map<String, Object> params) {
                BatchJobDefinition definition = definitionLookupService.findActiveByJobCode(jobCode);

                if (!definition.isImmediateExecutable()) {
                        throw new RuntimeException("このバッチは即時実行できません。 jobCode=" + jobCode);
                }

                return executeInternal(
                                definition,
                                params != null ? params : Map.of());
        }

        public BatchExecuteResponse executeScheduled(String jobCode) {
                BatchJobDefinition definition = definitionLookupService.findActiveByJobCode(jobCode);

                if (!definition.isScheduleEnabled()) {
                        throw new RuntimeException("このバッチはスケジュール実行できません。 jobCode=" + jobCode);
                }

                return executeInternal(definition, Map.of());
        }

        public List<BatchExecutionLogResponse> findLogs() {
                return logService.findLogs();
        }

        public List<BatchExecutionLogResponse> findLogsByJobCode(String jobCode) {
                return logService.findLogsByJobCode(jobCode);
        }

        public BatchJobRunResult executeNowForResult(
                        String jobCode,
                        Map<String, Object> params) {
                BatchJobDefinition definition = definitionLookupService.findActiveByJobCode(jobCode);

                if (!definition.isImmediateExecutable()) {
                        throw new RuntimeException("このバッチは即時実行できません。 jobCode=" + jobCode);
                }

                BatchExecutionLog executionLog = logService.start(definition);

                try {
                        BatchJobExecutionContext context = new BatchJobExecutionContext(
                                        definition,
                                        executionLog,
                                        params != null ? params : Map.of());

                        BatchJobExecutionResult result = executorResolver
                                        .resolve(definition.getJobType())
                                        .execute(context);

                        Instant finishedAt = Instant.now();

                        logService.complete(
                                        executionLog,
                                        result,
                                        finishedAt);

                        definition.setLastExecutedAt(finishedAt);
                        definitionRepository.save(definition);

                        return BatchJobRunResult.builder()
                                        .executionLogId(executionLog.getId())
                                        .result(result)
                                        .build();

                } catch (Exception e) {
                        logService.fail(executionLog, e);
                        throw e;
                }
        }

        private BatchExecuteResponse executeInternal(
                        BatchJobDefinition definition,
                        Map<String, Object> params) {
                BatchExecutionLog executionLog = logService.start(definition);

                try {
                        BatchJobExecutionContext context = new BatchJobExecutionContext(
                                        definition,
                                        executionLog,
                                        params);

                        BatchJobExecutionResult result = executorResolver
                                        .resolve(definition.getJobType())
                                        .execute(context);

                        Instant finishedAt = Instant.now();

                        logService.complete(
                                        executionLog,
                                        result,
                                        finishedAt);

                        definition.setLastExecutedAt(finishedAt);
                        definitionRepository.save(definition);

                        return responseBuilder.completed(
                                        definition,
                                        executionLog,
                                        result);

                } catch (Exception e) {
                        log.error(
                                        "Batch execution failed. jobCode={}, jobType={}, targetCode={}",
                                        definition.getJobCode(),
                                        definition.getJobType(),
                                        definition.getTargetCode(),
                                        e);

                        logService.fail(executionLog, e);

                        return responseBuilder.failed(
                                        definition,
                                        executionLog);
                }
        }
}