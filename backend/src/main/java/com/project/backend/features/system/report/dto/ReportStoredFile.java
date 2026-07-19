package com.project.backend.features.system.report.dto;

import com.project.backend.app.storage.enums.StorageType;

public record ReportStoredFile(
        StorageType storageType,
        String fileKey,
        String fileName,
        String contentType,
        Long fileSize
) {
}