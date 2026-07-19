package com.project.backend.features.system.report.service.api;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.service.converter.ReportRowNormalizer;
import com.project.backend.features.system.report.service.validation.OutputTableValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutputTableQueryService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final OutputTableValidator validator;
    private final ReportRowNormalizer rowNormalizer;

    @SuppressWarnings("null")
    public List<Map<String,Object>> findByExecutionId(
            String tableName,
            String executionId
    ) {

        validator.validateTableName(tableName);

        String sql =
                "SELECT * FROM "
                        + tableName
                        + " WHERE execution_id = :executionId ORDER BY 1";

        return rowNormalizer.normalize(
                jdbcTemplate.queryForList(
                        sql,
                        Map.of("executionId", executionId)
                )
        );
    }

    @SuppressWarnings("null")
    public List<Map<String,Object>> query(
            String sql,
            Map<String,Object> params
    ) {

        if (!StringUtils.hasText(sql)) {
            return List.of();
        }

        return rowNormalizer.normalize(
                jdbcTemplate.queryForList(
                        sql,
                        params != null
                                ? params
                                : Map.of()
                )
        );
    }
}