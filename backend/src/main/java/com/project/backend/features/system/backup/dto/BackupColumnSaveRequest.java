package com.project.backend.features.system.backup.dto;

import com.project.backend.features.system.backup.enums.BackupDataType;

public record BackupColumnSaveRequest(
        Long id,
        String columnName,
        String csvHeaderName,
        BackupDataType dataType,
        Boolean exportFlag,
        Integer orderNo
) {
}