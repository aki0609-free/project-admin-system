package com.project.backend.features.system.excelbook.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SnapshotMapQueryService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @SuppressWarnings("null")
    public List<Map<String, Object>> findRows(
            String sourceName,
            String targetMonth
    ) {
        validateIdentifier(sourceName);

        String sql = """
                SELECT *
                FROM %s
                WHERE target_month = :targetMonth
                ORDER BY customer_name, site_name
                """.formatted(sourceName);

        return jdbcTemplate.queryForList(
                sql,
                Map.of("targetMonth", targetMonth)
        );
    }

    public Map<String, Object> buildContext(
            String targetMonth,
            List<Map<String, Object>> rows
    ) {
        Map<String, Object> context = new HashMap<>();
        context.put("target_month", targetMonth);
        context.put("rows", rows);
        return context;
    }

    private void validateIdentifier(String value) {
        if (value == null || !value.matches("^[A-Za-z0-9_]+$")) {
            throw new IllegalArgumentException("不正なテーブル名です: " + value);
        }
    }
}