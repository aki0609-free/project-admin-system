package com.project.backend.features.system.backup.service.builder;

import java.util.List;
import java.util.regex.Pattern;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.backup.dto.BackupColumnDefinition;

@Component
public class BackupSqlBuilder {

    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    public String buildSelectSql(
            String tableName,
            List<BackupColumnDefinition> columns,
            boolean tenantScoped
    ) {
        validateIdentifier(tableName, "tableName");

        String selectColumns = columns.stream()
                .map(BackupColumnDefinition::columnName)
                .peek(columnName -> validateIdentifier(columnName, "columnName"))
                .reduce((a, b) -> a + ", " + b)
                .orElseThrow();

        String sql = "SELECT " + selectColumns + " FROM " + tableName;

        if (tenantScoped) {
            sql += " WHERE tenant_id = :tenantId";
        }

        return sql;
    }

    public Map<String, Object> buildParameters(
            boolean tenantScoped,
            String tenantId
    ) {
        if (!tenantScoped) {
            return Map.of();
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new RuntimeException("テナント情報を取得できません。");
        }
        return Map.of("tenantId", tenantId);
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
