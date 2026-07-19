package com.project.backend.features.system.backup.dto;

import java.time.Instant;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.backup.entity.BackupHistory;
import com.project.backend.features.system.backup.enums.BackupHistoryStatus;

import lombok.Builder;

@Builder
public record BackupHistoryResponse(
        Long id,
        String targetCodes,
        String fileName,
        String contentType,
        Long fileSize,
        boolean zipOutput,
        StorageType storageType,
        String storedFileKey,
        String storedFileName,
        BackupHistoryStatus status,
        String executedBy,
        Instant executedAt,
        String errorMessage
) {
    public static BackupHistoryResponse from(BackupHistory entity) {
        return BackupHistoryResponse.builder()
                .id(entity.getId())
                .targetCodes(entity.getTargetCodes())
                .fileName(entity.getFileName())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .zipOutput(entity.isZipOutput())
                .storageType(entity.getStorageType())
                .storedFileKey(entity.getStoredFileKey())
                .storedFileName(entity.getStoredFileName())
                .status(entity.getStatus())
                .executedBy(entity.getExecutedBy())
                .executedAt(entity.getExecutedAt())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
}