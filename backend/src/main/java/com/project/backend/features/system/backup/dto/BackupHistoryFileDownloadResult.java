package com.project.backend.features.system.backup.dto;

import lombok.Builder;

@Builder
public record BackupHistoryFileDownloadResult(
        String fileName,
        String contentType,
        byte[] data
) {
}