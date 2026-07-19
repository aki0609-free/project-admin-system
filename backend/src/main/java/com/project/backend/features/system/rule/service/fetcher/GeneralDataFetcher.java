package com.project.backend.features.system.rule.service.fetcher;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.rule.entity.RuleDataSource;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeneralDataFetcher {

    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @SuppressWarnings("null")
    public List<Map<String, Object>> fetch(
            RuleDataSource source,
            Map<String, Object> params
    ) {
        validateIdentifier(source.getTableName(), "tableName");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ")
                .append(source.getTableName());

        if (StringUtils.hasText(source.getWhereClause())) {
            sql.append(" WHERE ")
                    .append(source.getWhereClause());
        }

        return jdbcTemplate.queryForList(
                sql.toString(),
                params != null ? params : Map.of()
        );
    }

    private void validateIdentifier(
            String value,
            String label
    ) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException(label + " は必須です。");
        }

        if (!SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new RuntimeException(
                    label + " に使用できない文字が含まれています。 value=" + value
            );
        }
    }
}