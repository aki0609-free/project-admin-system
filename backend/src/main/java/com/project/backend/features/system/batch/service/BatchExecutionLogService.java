package com.project.backend.features.system.batch.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.batch.dto.BatchExecutionLogResponse;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.entity.BatchExecutionLog;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.enums.BatchExecutionStatus;
import com.project.backend.features.system.batch.enums.BatchExecutionTrigger;
import com.project.backend.features.system.batch.mapper.BatchJobMapper;
import com.project.backend.features.system.batch.repository.BatchExecutionLogRepository;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchExecutionLogService {

    private final BatchExecutionLogRepository repository;
    private final BatchJobMapper mapper;
    private final BatchExecutionParameterService parameterService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<BatchExecutionLogResponse> findLogs() {
        return mapper.toLogResponseList(
                repository.findTop200ByTenantIdAndDeletedAtIsNullOrderByIdDesc(
                        requireTenantId()
                )
        );
    }

    @Transactional(readOnly = true)
    public List<BatchExecutionLogResponse> findLogsByJobCode(String jobCode) {
        return mapper.toLogResponseList(
                repository.findTop200ByTenantIdAndJobCodeAndDeletedAtIsNullOrderByIdDesc(
                        requireTenantId(),
                        jobCode
                )
        );
    }

    @Transactional(readOnly = true)
    public BatchExecutionLog findRetryTarget(Long logId) {
        BatchExecutionLog log = repository.findByIdAndTenantIdAndDeletedAtIsNull(
                        logId,
                        requireTenantId()
                )
                .orElseThrow(() -> new RuntimeException(
                        "バッチ実行履歴が見つかりません。 logId=" + logId
                ));

        if (log.getStatus() != BatchExecutionStatus.FAILED) {
            throw new RuntimeException(
                    "再実行できるのは失敗した履歴だけです。 logId=" + logId
            );
        }

        return log;
    }

    public BatchExecutionLog start(
            BatchJobDefinition definition,
            BatchExecutionTrigger triggerType,
            Map<String, Object> params,
            Long retrySourceLogId
    ) {
        BatchExecutionLog log = new BatchExecutionLog();

        log.setJobCode(definition.getJobCode());
        log.setJobName(definition.getJobName());
        log.setJobType(definition.getJobType());
        log.setTargetCode(definition.getTargetCode());
        log.setStatus(BatchExecutionStatus.STARTED);
        log.setTriggerType(triggerType);
        log.setExecutedBy(currentUsername(triggerType));
        log.setParametersJson(parameterService.serialize(params));
        log.setRetrySourceLogId(retrySourceLogId);
        log.setStartedAt(Instant.now(clock));
        log.setMessage("バッチ実行を開始しました。");

        return repository.save(log);
    }

    public Map<String, Object> restoreParameters(BatchExecutionLog log) {
        return parameterService.deserialize(log.getParametersJson());
    }

    public void complete(
            BatchExecutionLog log,
            BatchJobExecutionResult result,
            Instant finishedAt
    ) {
        log.setStatus(BatchExecutionStatus.COMPLETED);
        log.setFinishedAt(finishedAt);
        log.setMessage(result.message());
        log.setStorageType(result.storageType());
        log.setOutputFileKey(result.outputFileKey());
        log.setOutputFileName(result.outputFileName());
        log.setContentType(result.contentType());
        log.setFileSize(result.fileSize());

        repository.save(log);
    }

    public void fail(
            BatchExecutionLog log,
            Exception e
    ) {
        log.setStatus(BatchExecutionStatus.FAILED);
        log.setFinishedAt(Instant.now(clock));
        log.setErrorMessage(limit(e.getMessage(), 4000));

        repository.save(log);
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }

    private String currentUsername(BatchExecutionTrigger triggerType) {
        if (triggerType == BatchExecutionTrigger.SCHEDULED) {
            return "system";
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return "system";
        }

        return authentication.getName();
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new RuntimeException("テナント情報を取得できません。");
        }
        return tenantId;
    }
}
