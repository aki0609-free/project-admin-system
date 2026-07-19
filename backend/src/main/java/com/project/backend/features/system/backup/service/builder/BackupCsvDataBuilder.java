package com.project.backend.features.system.backup.service.builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.backup.dto.BackupColumnDefinition;

@Component
public class BackupCsvDataBuilder {

    public List<Map<String, Object>> build(
            List<Map<String, Object>> rows,
            List<BackupColumnDefinition> columns
    ) {
        return rows.stream()
                .map(row -> buildRow(row, columns))
                .toList();
    }

    private Map<String, Object> buildRow(
            Map<String, Object> row,
            List<BackupColumnDefinition> columns
    ) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (BackupColumnDefinition column : columns) {
            result.put(
                    column.columnName(),
                    row.get(column.columnName())
            );
        }

        return result;
    }
}