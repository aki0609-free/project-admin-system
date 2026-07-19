package com.project.backend.features.system.imports.service.executor;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.project.backend.features.system.imports.dto.ImportColumnDefinition;
import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.service.builder.ImportSqlBuilder;
import com.project.backend.features.system.imports.service.resolver.ImportColumnResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportWriteExecutor {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ImportSqlBuilder sqlBuilder;
    private final ImportColumnResolver columnResolver;

    @SuppressWarnings("null")
    public void executeDeleteAll(
            ImportTargetDefinition target
    ) {
        String sql =
                sqlBuilder.buildDeleteAllSql(
                        target.tableName()
                );

        jdbcTemplate.update(
                sql,
                Map.of()
        );
    }

    @SuppressWarnings("null")
    public void executeInsert(
            ImportTargetDefinition target,
            List<ImportColumnDefinition> columns,
            Map<String, Object> params
    ) {
        String sql =
                sqlBuilder.buildInsertSql(
                        target.tableName(),
                        columns
                );

        jdbcTemplate.update(
                sql,
                params
        );
    }

    @SuppressWarnings("null")
    public int executeUpdate(
            ImportTargetDefinition target,
            List<ImportColumnDefinition> columns,
            Map<String, Object> params
    ) {
        List<ImportColumnDefinition> keyColumns =
                columnResolver.resolveKeyColumns(
                        target,
                        columns
                );

        List<ImportColumnDefinition> updateColumns =
                columnResolver.resolveUpdateColumns(
                        target,
                        columns
                );

        String sql =
                sqlBuilder.buildUpdateSql(
                        target.tableName(),
                        keyColumns,
                        updateColumns
                );

        return jdbcTemplate.update(
                sql,
                params
        );
    }

    public boolean exists(
            ImportTargetDefinition target,
            List<ImportColumnDefinition> columns,
            Map<String, Object> params
    ) {
        List<ImportColumnDefinition> keyColumns =
                columnResolver.resolveKeyColumns(
                        target,
                        columns
                );

        String sql =
                sqlBuilder.buildExistsSql(
                        target.tableName(),
                        keyColumns
                );

        @SuppressWarnings("null")
        Integer count =
                jdbcTemplate.queryForObject(
                        sql,
                        params,
                        Integer.class
                );

        return count != null && count > 0;
    }
}