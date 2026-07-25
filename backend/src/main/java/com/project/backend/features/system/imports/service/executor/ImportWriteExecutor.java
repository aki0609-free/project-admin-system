package com.project.backend.features.system.imports.service.executor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.imports.dto.ImportColumnDefinition;
import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.entity.ImportTargetCatalog;
import com.project.backend.features.system.imports.service.ImportTargetCatalogService;
import com.project.backend.features.system.imports.service.builder.ImportSqlBuilder;
import com.project.backend.features.system.imports.service.resolver.ImportColumnResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportWriteExecutor {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ImportSqlBuilder sqlBuilder;
    private final ImportColumnResolver columnResolver;
    private final ImportTargetCatalogService catalogService;

    @SuppressWarnings("null")
    public void executeDeleteAll(
            ImportTargetDefinition target
    ) {
        ImportTargetCatalog catalog =
                catalogService.findRequired(target.tableName());

        if (!catalog.isAllowDeleteInsertFlag()) {
            throw new IllegalArgumentException(
                    "DELETE_INSERTが許可されていない取込先です。 tableName="
                            + target.tableName()
            );
        }

        String sql =
                sqlBuilder.buildDeleteSql(
                        target.tableName(),
                        catalog.isTenantScopedFlag()
                );

        Map<String, Object> parameters =
                catalog.isTenantScopedFlag()
                        ? Map.of(
                                "tenantId",
                                requireTenantId()
                        )
                        : Map.of();

        jdbcTemplate.update(
                sql,
                parameters
        );
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();

        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException(
                    "テナントが確定していないためDELETE_INSERTを実行できません。"
            );
        }

        return tenantId;
    }

    @SuppressWarnings("null")
    public void executeInsert(
            ImportTargetDefinition target,
            List<ImportColumnDefinition> columns,
            Map<String, Object> params
    ) {
        ImportTargetCatalog catalog =
                catalogService.findRequired(target.tableName());
        Map<String, Object> writeParameters =
                withTechnicalParameters(
                        params,
                        catalog.isTenantScopedFlag()
                );

        String sql =
                sqlBuilder.buildInsertSql(
                        target.tableName(),
                        columns,
                        catalog.isTenantScopedFlag()
                );

        jdbcTemplate.update(
                sql,
                writeParameters
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

        ImportTargetCatalog catalog =
                catalogService.findRequired(target.tableName());
        Map<String, Object> writeParameters =
                withTechnicalParameters(
                        params,
                        catalog.isTenantScopedFlag()
                );

        String sql =
                sqlBuilder.buildUpdateSql(
                        target.tableName(),
                        keyColumns,
                        updateColumns,
                        catalog.isTenantScopedFlag()
                );

        return jdbcTemplate.update(
                sql,
                writeParameters
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

        ImportTargetCatalog catalog =
                catalogService.findRequired(target.tableName());
        Map<String, Object> writeParameters =
                withTechnicalParameters(
                        params,
                        catalog.isTenantScopedFlag()
                );

        String sql =
                sqlBuilder.buildExistsSql(
                        target.tableName(),
                        keyColumns,
                        catalog.isTenantScopedFlag()
                );

        @SuppressWarnings("null")
        Integer count =
                jdbcTemplate.queryForObject(
                        sql,
                        writeParameters,
                        Integer.class
                );

        return count != null && count > 0;
    }

    private Map<String, Object> withTechnicalParameters(
            Map<String, Object> parameters,
            boolean tenantScoped
    ) {
        if (!tenantScoped) {
            return parameters;
        }

        Map<String, Object> resolved =
                new HashMap<>(parameters);
        resolved.put("__tenantId", requireTenantId());
        resolved.put("__now", Instant.now());
        return resolved;
    }
}
