package com.project.backend.features.system.rule.service.fetcher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.rule.entity.RuleDataSource;
import com.project.backend.features.system.rule.entity.RuleDataSourceCatalog;
import com.project.backend.features.system.rule.service.RuleDataSourceCatalogService;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeneralDataFetcher {

    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RuleDataSourceCatalogService catalogService;

    @SuppressWarnings("null")
    public List<Map<String, Object>> fetch(
            RuleDataSource source,
            Map<String, Object> params
    ) {
        RuleDataSourceCatalog catalog = loadCatalog(source);
        String tableName = catalog != null
                ? catalog.getPhysicalName()
                : source.getTableName();
        String whereClause = catalog != null
                ? catalog.getWhereClauseTemplate()
                : source.getWhereClause();
        int maxRows = resolveMaxRows(source, catalog);

        validateIdentifier(tableName, "tableName");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append(selectColumns(source))
                .append(" FROM ")
                .append(tableName);

        if (StringUtils.hasText(whereClause)) {
            sql.append(" WHERE ")
                    .append(whereClause);
        }

        sql.append(" LIMIT :__ruleLimit");

        Map<String, Object> queryParameters = new LinkedHashMap<>();

        if (params != null) {
            queryParameters.putAll(params);
        }

        if (catalog != null && catalog.isTenantScopedFlag()) {
            String tenantId = TenantContext.getTenantId();

            if (!StringUtils.hasText(tenantId)) {
                throw new IllegalStateException(
                        "RuleデータソースのtenantIdが未設定です。"
                );
            }

            if (!StringUtils.hasText(whereClause)
                    || !whereClause.contains(":tenantId")) {
                throw new IllegalStateException(
                        "テナント対象のRuleデータソースに:tenantId条件がありません。 sourceCode="
                                + catalog.getSourceCode()
                );
            }

            queryParameters.put("tenantId", tenantId);
        }

        queryParameters.put(
                "__ruleLimit",
                source.isSingleRowFlag() ? 2 : maxRows + 1
        );

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(
                sql.toString(),
                queryParameters
        );

        if (!source.isSingleRowFlag()
                && rows.size() > maxRows) {
            throw new IllegalArgumentException(
                    "Ruleデータソースの取得件数が上限を超えました。 sourceName="
                            + source.getSourceName()
                            + ", maxRows="
                            + maxRows
            );
        }

        return rows;
    }

    private String selectColumns(RuleDataSource source) {
        if (source.getColumns() == null
                || source.getColumns().isEmpty()) {
            return "*";
        }

        String columns = source.getColumns().stream()
                .filter(column -> column.getDeletedAt() == null)
                .map(column -> {
                    validateIdentifier(
                            column.getColumnName(),
                            "columnName"
                    );
                    return column.getColumnName();
                })
                .distinct()
                .collect(Collectors.joining(", "));

        return StringUtils.hasText(columns) ? columns : "*";
    }

    private RuleDataSourceCatalog loadCatalog(
            RuleDataSource source
    ) {
        if (!StringUtils.hasText(source.getCatalogCode())) {
            return null;
        }

        return catalogService.findRequired(
                source.getCatalogCode()
        );
    }

    private int resolveMaxRows(
            RuleDataSource source,
            RuleDataSourceCatalog catalog
    ) {
        if (source.isSingleRowFlag()) {
            return 1;
        }

        if (catalog == null) {
            return 1000;
        }

        if (catalog.getMaxRows() < 1
                || catalog.getMaxRows() > 1000) {
            throw new IllegalStateException(
                    "RuleデータソースカタログのmaxRowsが不正です。 sourceCode="
                            + catalog.getSourceCode()
            );
        }

        return catalog.getMaxRows();
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
