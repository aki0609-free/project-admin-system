package com.project.backend.features.system.imports.service.builder;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.imports.dto.ImportColumnDefinition;

@Component
public class ImportSqlBuilder {

    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    public String buildInsertSql(
            String tableName,
            List<ImportColumnDefinition> columns
    ) {
        validateIdentifier(tableName, "tableName");

        String columnNames = columns.stream()
                .map(ImportColumnDefinition::columnName)
                .peek(columnName -> validateIdentifier(columnName, "columnName"))
                .collect(Collectors.joining(", "));

        String paramNames = columns.stream()
                .map(ImportColumnDefinition::columnName)
                .map(columnName -> ":" + columnName)
                .collect(Collectors.joining(", "));

        return "INSERT INTO " + tableName
                + " (" + columnNames + ") VALUES (" + paramNames + ")";
    }

    public String buildUpdateSql(
            String tableName,
            List<ImportColumnDefinition> keyColumns,
            List<ImportColumnDefinition> updateColumns
    ) {
        validateIdentifier(tableName, "tableName");

        String setClause = updateColumns.stream()
                .map(ImportColumnDefinition::columnName)
                .peek(columnName -> validateIdentifier(columnName, "columnName"))
                .map(columnName -> columnName + " = :" + columnName)
                .collect(Collectors.joining(", "));

        String whereClause = buildWhereClause(keyColumns);

        return "UPDATE " + tableName
                + " SET " + setClause
                + " WHERE " + whereClause;
    }

    public String buildExistsSql(
            String tableName,
            List<ImportColumnDefinition> keyColumns
    ) {
        validateIdentifier(tableName, "tableName");

        return "SELECT COUNT(*) FROM "
                + tableName
                + " WHERE "
                + buildWhereClause(keyColumns);
    }

    public String buildDeleteAllSql(String tableName) {
        validateIdentifier(tableName, "tableName");
        return "DELETE FROM " + tableName;
    }

    private String buildWhereClause(
            List<ImportColumnDefinition> keyColumns
    ) {
        return keyColumns.stream()
                .map(ImportColumnDefinition::columnName)
                .peek(columnName -> validateIdentifier(columnName, "columnName"))
                .map(columnName -> columnName + " = :" + columnName)
                .collect(Collectors.joining(" AND "));
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