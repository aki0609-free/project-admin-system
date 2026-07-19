package com.project.backend.features.system.backup.dto;

import com.project.backend.features.system.backup.enums.BackupDataType;

import lombok.Builder;

@Builder
public record BackupColumnDefinition(
        Long id,
        String columnName,
        String csvHeaderName,
        BackupDataType dataType,
        boolean exportFlag,
        int orderNo
) {
}