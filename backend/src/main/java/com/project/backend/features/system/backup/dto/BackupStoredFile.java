package com.project.backend.features.system.backup.dto;

import com.project.backend.app.storage.enums.StorageType;

public record BackupStoredFile(
        StorageType storageType,
        String fileKey,
        String fileName,
        String contentType,
        Long fileSize
) {
}