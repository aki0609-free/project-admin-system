package com.project.backend.features.system.backup.dto;

import com.project.backend.features.system.backup.enums.BackupOutputMode;

import lombok.Builder;

@Builder
public record SingleBackupFile(
        String targetCode,
        String targetName,
        String fileName,
        byte[] data,
        boolean zipRequired,
        BackupOutputMode outputMode,
        String outputDir
) {
}