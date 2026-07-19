package com.project.backend.features.system.backup.dto;

import java.util.List;

import com.project.backend.features.system.backup.enums.BackupOutputMode;

public record BackupTargetSaveRequest(
        String targetCode,
        String targetName,
        String tableName,
        String description,
        BackupOutputMode outputMode,
        String outputDir,
        String fileNamePattern,
        Boolean zipRequired,
        Boolean includeHeader,
        Boolean backupEnabled,
        Boolean activeFlag,
        List<BackupColumnSaveRequest> columns
) {
}