package com.project.backend.features.system.backup.dto;

import java.util.List;

import com.project.backend.features.system.backup.enums.BackupOutputMode;

import lombok.Builder;

@Builder
public record BackupTargetResponse(
        Long id,
        String targetCode,
        String targetName,
        String tableName,
        String description,
        BackupOutputMode outputMode,
        String outputDir,
        String fileNamePattern,
        boolean zipRequired,
        boolean includeHeader,
        boolean backupEnabled,
        boolean activeFlag,
        List<BackupColumnDefinition> columns
) {
}