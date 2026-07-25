package com.project.backend.features.system.backup.service.validation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.backup.dto.BackupSourceSchema;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BackupSchemaInspector {

    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,199}");

    private final DataSource dataSource;

    public BackupSourceSchema inspect(String tableName) {
        validateIdentifier(tableName, "tableName");

        String sql = "SELECT * FROM " + tableName + " WHERE 1 = 0";

        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            ResultSetMetaData metadata = resultSet.getMetaData();
            Set<String> columns = new LinkedHashSet<>();

            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                columns.add(metadata.getColumnName(index).toLowerCase());
            }

            return new BackupSourceSchema(
                    tableName,
                    Set.copyOf(columns),
                    columns.contains("tenant_id")
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "バックアップ対象テーブルを確認できません。 tableName=" + tableName,
                    e
            );
        }
    }

    public void validateIdentifier(String value, String label) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new RuntimeException(
                    label + " に使用できない文字が含まれています。 value=" + value
            );
        }
    }
}
