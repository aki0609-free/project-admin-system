package com.project.backend.features.system.backup.dto;

import lombok.Builder;

@Builder
public record BackupExecutionResult(
        String fileName,
        String contentType,
        byte[] data,
        boolean zipOutput,
        BackupStoredFile storedFile
) {
}