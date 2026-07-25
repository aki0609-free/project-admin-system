package com.project.backend.features.system.backup.dto;

import java.util.Set;

public record BackupSourceSchema(
        String tableName,
        Set<String> columnNames,
        boolean tenantScoped
) {
    public boolean containsColumn(String columnName) {
        if (columnName == null) {
            return false;
        }
        return columnNames.contains(columnName.toLowerCase());
    }
}
