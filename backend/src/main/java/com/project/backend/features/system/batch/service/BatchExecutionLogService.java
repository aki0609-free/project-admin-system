package com.project.backend.features.system.batch.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.batch.dto.BatchExecutionLogResponse;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.entity.BatchExecutionLog;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.enums.BatchExecutionStatus;
import com.project.backend.features.system.batch.mapper.BatchJobMapper;
import com.project.backend.features.system.batch.repository.BatchExecutionLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchExecutionLogService {

    private final BatchExecutionLogRepository repository;
    private final BatchJobMapper mapper;

    @Transactional(readOnly = true)
    public List<BatchExecutionLogResponse> findLogs() {
        return mapper.toLogResponseList(
                repository.findAllByDeletedAtIsNullOrderByIdDesc()
        );
    }

    @Transactional(readOnly = true)
    public List<BatchExecutionLogResponse> findLogsByJobCode(String jobCode) {
        return mapper.toLogResponseList(
                repository.findByJobCodeAndDeletedAtIsNullOrderByIdDesc(jobCode)
        );
    }

    public BatchExecutionLog start(BatchJobDefinition definition) {
        BatchExecutionLog log = new BatchExecutionLog();

        log.setJobCode(definition.getJobCode());
        log.setJobName(definition.getJobName());
        log.setJobType(definition.getJobType());
        log.setTargetCode(definition.getTargetCode());
        log.setStatus(BatchExecutionStatus.STARTED);
        log.setStartedAt(Instant.now());
        log.setMessage("バッチ実行を開始しました。");

        return repository.save(log);
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
        log.setFinishedAt(Instant.now());
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
}