package com.project.backend.app.sql.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.app.sql.dto.TableColumnMeta;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DynamicTableInsertService {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[A-Za-z0-9_]+$");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void insert(String tableName, Map<String, Object> values) {
        validateTableName(tableName);

        List<TableColumnMeta> columns = loadTableColumns(tableName);
        if (columns.isEmpty()) {
            throw new RuntimeException("テーブル定義が見つかりません: " + tableName);
        }

        Map<String, Object> bind = buildInsertBind(columns, values);
        if (bind.isEmpty()) {
            throw new RuntimeException("INSERT対象の値がありません: " + tableName);
        }

        validateRequiredColumns(columns, bind);

        String columnSql = String.join(", ", bind.keySet());
        String valueSql = bind.keySet().stream()
                .map(name -> ":" + name)
                .collect(Collectors.joining(", "));

        String sql = "INSERT INTO " + tableName + " (" + columnSql + ") VALUES (" + valueSql + ")";
        jdbcTemplate.update(sql, bind);
    }

    @SuppressWarnings({ "null", "unchecked" })
    public void insertBatch(String tableName, List<Map<String, Object>> rows) {
        validateTableName(tableName);

        if (rows == null || rows.isEmpty()) {
            throw new RuntimeException("INSERT対象データが空です。");
        }

        List<TableColumnMeta> columns = loadTableColumns(tableName);

        if (columns.isEmpty()) {
            throw new RuntimeException("テーブル定義が見つかりません: " + tableName);
        }

        List<Map<String, Object>> binds = rows.stream()
                .map(row -> {
                    Map<String, Object> bind = buildInsertBind(columns, row);

                    validateRequiredColumns(columns, bind);

                    return bind;
                })
                .toList();

        Map<String, Object> firstBind = binds.get(0);

        String columnSql = String.join(", ", firstBind.keySet());

        String valueSql = firstBind.keySet().stream()
                .map(name -> ":" + name)
                .collect(Collectors.joining(", "));

        String sql = "INSERT INTO " + tableName
                + " (" + columnSql + ") VALUES (" + valueSql + ")";

        jdbcTemplate.batchUpdate(
                sql,
                binds.toArray(new Map[0]));
    }

    @SuppressWarnings("null")
    private List<TableColumnMeta> loadTableColumns(String tableName) {
        String sql = """
                    SELECT
                        column_name,
                        ordinal_position,
                        is_nullable,
                        data_type,
                        column_default,
                        extra
                    FROM information_schema.columns
                    WHERE table_schema = DATABASE()
                      AND table_name = :tableName
                    ORDER BY ordinal_position
                """;

        return jdbcTemplate.query(
                sql,
                Map.of("tableName", tableName),
                (rs, rowNum) -> new TableColumnMeta(
                        rs.getString("column_name"),
                        rs.getInt("ordinal_position"),
                        rs.getString("is_nullable"),
                        rs.getString("data_type"),
                        rs.getString("column_default"),
                        rs.getString("extra")));
    }

    private Map<String, Object> buildInsertBind(List<TableColumnMeta> columns, Map<String, Object> values) {
        Map<String, Object> bind = new LinkedHashMap<>();

        for (TableColumnMeta column : columns) {
            String columnName = column.columnName();

            if (column.isAutoIncrement()) {
                continue;
            }

            if (!values.containsKey(columnName)) {
                continue;
            }

            Object rawValue = values.get(columnName);
            bind.put(columnName, normalizeValue(rawValue));
        }

        return bind;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return Timestamp.valueOf(localDateTime);
        }
        return value;
    }

    private void validateRequiredColumns(List<TableColumnMeta> columns, Map<String, Object> bind) {
        List<String> missingRequiredColumns = columns.stream()
                .filter(column -> !column.isAutoIncrement())
                .filter(column -> !column.isNullableColumn())
                .filter(column -> !column.hasDefaultValue())
                .map(TableColumnMeta::columnName)
                .filter(columnName -> !bind.containsKey(columnName))
                .toList();

        if (!missingRequiredColumns.isEmpty()) {
            throw new RuntimeException("必須カラムが不足しています: " + String.join(", ", missingRequiredColumns));
        }
    }

    private void validateTableName(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            throw new RuntimeException("テーブル名は必須です。");
        }

        if (!SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new RuntimeException("不正なテーブル名です: " + tableName);
        }
    }
}