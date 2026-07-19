package com.project.backend.features.system.backup.service.builder;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.backup.dto.BackupColumnDefinition;

@Component
public class BackupSqlBuilder {

    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    public String buildSelectSql(
            String tableName,
            List<BackupColumnDefinition> columns
    ) {
        validateIdentifier(tableName, "tableName");

        String selectColumns = columns.stream()
                .map(BackupColumnDefinition::columnName)
                .peek(columnName -> validateIdentifier(columnName, "columnName"))
                .reduce((a, b) -> a + ", " + b)
                .orElseThrow();

        return "SELECT " + selectColumns + " FROM " + tableName;
    }

    private void validateIdentifier(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(label + " が空です。");
        }

        if (!SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new RuntimeException(label + " に使用できない文字が含まれています。 value=" + value);
        }
    }
}