package com.project.backend.features.system.batch.dto;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.batch.enums.BatchExecutionStatus;

import lombok.Builder;

@Builder
public record BatchExecuteResponse(
        String jobCode,
        BatchExecutionStatus status,
        String message,
        Long logId,
        StorageType storageType,
        String outputFileKey,
        String outputFileName,
        String contentType,
        Long fileSize
) {
}