package com.project.backend.features.system.report.service.converter;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ReportRowNormalizer {

    public List<Map<String,Object>> normalize(
            List<Map<String,Object>> rows
    ) {
        return rows.stream()
                .map(this::normalizeRow)
                .toList();
    }

    private Map<String,Object> normalizeRow(
            Map<String,Object> row
    ) {
        Map<String,Object> result =
                new LinkedHashMap<>();

        row.forEach((k,v) ->
                result.put(k, normalizeValue(v)));

        return result;
    }

    private Object normalizeValue(
            Object value
    ) {
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }

        return value;
    }
}
