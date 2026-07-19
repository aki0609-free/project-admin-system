package com.project.backend.app.sql.service;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProcedureExecutorService {

    private static final Pattern SAFE_PROCEDURE_NAME = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final String EXECUTION_ID_PARAM = "executionId";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @SuppressWarnings("null")
    public void executeByExecutionId(String procedureName, String executionId) {
        validateProcedureName(procedureName);

        if (!StringUtils.hasText(executionId)) {
            throw new RuntimeException("executionId は必須です。");
        }

        String sql = "CALL " + procedureName + "(:executionId)";
        jdbcTemplate.update(sql, Map.of(EXECUTION_ID_PARAM, executionId));
    }

    private void validateProcedureName(String procedureName) {
        if (!StringUtils.hasText(procedureName)) {
            throw new RuntimeException("プロシージャ名が未設定です。");
        }

        if (!SAFE_PROCEDURE_NAME.matcher(procedureName).matches()) {
            throw new RuntimeException("不正なプロシージャ名です: " + procedureName);
        }
    }
}