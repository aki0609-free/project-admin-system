package com.project.backend.features.system.batch.service.builder;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.batch.dto.BatchExecuteResponse;
import com.project.backend.features.system.batch.dto.BatchJobExecutionResult;
import com.project.backend.features.system.batch.entity.BatchExecutionLog;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.enums.BatchExecutionStatus;

@Component
public class BatchExecutionResponseBuilder {

    public BatchExecuteResponse completed(
            BatchJobDefinition definition,
            BatchExecutionLog log,
            BatchJobExecutionResult result
    ) {
        return BatchExecuteResponse.builder()
                .jobCode(definition.getJobCode())
                .status(BatchExecutionStatus.COMPLETED)
                .message(result.message())
                .logId(log.getId())
                .storageType(result.storageType())
                .outputFileKey(result.outputFileKey())
                .outputFileName(result.outputFileName())
                .contentType(result.contentType())
                .fileSize(result.fileSize())
                .build();
    }

    public BatchExecuteResponse failed(
            BatchJobDefinition definition,
            BatchExecutionLog log
    ) {
        return BatchExecuteResponse.builder()
                .jobCode(definition.getJobCode())
                .status(BatchExecutionStatus.FAILED)
                .message("バッチ実行に失敗しました。")
                .logId(log.getId())
                .build();
    }
}