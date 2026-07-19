package com.project.backend.features.system.report.service.api;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportSqlExecutorService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @SuppressWarnings("null")
    public void execute(String sql, Map<String, Object> params) {
        if (!StringUtils.hasText(sql)) {
            throw new RuntimeException("SQL が未設定です。");
        }
        jdbcTemplate.update(sql, params != null ? params : Map.of());
    }

    public void executeByExecutionId(String sql, String executionId) {
        execute(sql, Map.of("executionId", executionId));
    }
}