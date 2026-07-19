package com.project.backend.features.system.batch.dto;

import java.time.Instant;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.batch.enums.BatchExecutionStatus;
import com.project.backend.features.system.batch.enums.BatchJobType;

import lombok.Builder;

@Builder
public record BatchExecutionLogResponse(
        Long id,
        String jobCode,
        String jobName,
        BatchJobType jobType,
        String targetCode,
        BatchExecutionStatus status,
        Instant startedAt,
        Instant finishedAt,
        String message,
        String errorMessage,
        StorageType storageType,
        String outputFileKey,
        String outputFileName,
        String contentType,
        Long fileSize
) {
}