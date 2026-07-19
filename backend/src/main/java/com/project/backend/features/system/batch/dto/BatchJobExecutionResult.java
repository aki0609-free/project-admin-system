package com.project.backend.features.system.batch.dto;

import com.project.backend.app.storage.enums.StorageType;

import lombok.Builder;

@Builder
public record BatchJobExecutionResult(
        String message,
        StorageType storageType,
        String outputFileKey,
        String outputFileName,
        String contentType,
        Long fileSize
) {
    public static BatchJobExecutionResult message(String message) {
        return BatchJobExecutionResult.builder()
                .message(message)
                .build();
    }
}