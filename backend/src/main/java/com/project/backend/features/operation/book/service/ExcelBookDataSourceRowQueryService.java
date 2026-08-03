package com.project.backend.features.operation.book.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.excelbook.entity.ExcelBookDataSourceCatalog;
import com.project.backend.features.system.excelbook.entity.ExcelBookVariableMapping;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExcelBookDataSourceRowQueryService {

    private static final Pattern IDENTIFIER =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern NAMED_PARAMETER =
            Pattern.compile("(?<!:):([A-Za-z][A-Za-z0-9_]*)");
    private static final Pattern DANGEROUS_SQL =
            Pattern.compile(
                    "\\b(SELECT|UNION|INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|CALL|SLEEP|BENCHMARK|OUTFILE|LOAD_FILE)\\b",
                    Pattern.CASE_INSENSITIVE
            );
    private static final Set<String> ALLOWED_PARAMETERS =
            Set.of("tenantId", "targetMonth");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> findRows(
            ExcelBookDataSourceCatalog catalog,
            List<ExcelBookVariableMapping> mappings,
            String targetMonth
    ) {
        LinkedHashSet<String> selectedColumns = mappings.stream()
                .map(ExcelBookVariableMapping::getSourceColumn)
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new
                ));
        return findRows(catalog, selectedColumns, targetMonth);
    }

    /**
     * 固定レイアウト用に、カタログで許可された有効列をすべて取得する。
     */
    public List<Map<String, Object>> findAllRows(
            ExcelBookDataSourceCatalog catalog,
            String targetMonth
    ) {
        if (catalog == null) {
            throw new IllegalArgumentException(
                    "台帳データソースは必須です。"
            );
        }
        LinkedHashSet<String> selectedColumns =
                catalog.getColumns().stream()
                        .filter(column ->
                                column.isActiveFlag()
                                        && column.getDeletedAt() == null
                        )
                        .sorted(java.util.Comparator.comparingInt(column ->
                                column.getOrderNo() == null
                                        ? Integer.MAX_VALUE
                                        : column.getOrderNo()
                        ))
                        .map(column -> column.getColumnName())
                        .collect(java.util.stream.Collectors.toCollection(
                                LinkedHashSet::new
                        ));
        return findRows(catalog, selectedColumns, targetMonth);
    }

    public List<Map<String, Object>> findAllRows(
            ExcelBookDataSourceCatalog catalog,
            String targetMonth,
            String selectionColumn,
            List<String> selectionValues
    ) {
        LinkedHashSet<String> selectedColumns =
                catalog.getColumns().stream()
                        .filter(column ->
                                column.isActiveFlag()
                                        && column.getDeletedAt() == null
                        )
                        .sorted(java.util.Comparator.comparingInt(column ->
                                column.getOrderNo() == null
                                        ? Integer.MAX_VALUE
                                        : column.getOrderNo()
                        ))
                        .map(column -> column.getColumnName())
                        .collect(java.util.stream.Collectors.toCollection(
                                LinkedHashSet::new
                        ));
        return findRows(
                catalog,
                selectedColumns,
                targetMonth,
                selectionColumn,
                selectionValues
        );
    }

    private List<Map<String, Object>> findRows(
            ExcelBookDataSourceCatalog catalog,
            LinkedHashSet<String> selectedColumns,
            String targetMonth
    ) {
        return findRows(
                catalog,
                selectedColumns,
                targetMonth,
                null,
                List.of()
        );
    }

    private List<Map<String, Object>> findRows(
            ExcelBookDataSourceCatalog catalog,
            LinkedHashSet<String> selectedColumns,
            String targetMonth,
            String selectionColumn,
            List<String> selectionValues
    ) {
        if (catalog == null) {
            throw new IllegalArgumentException(
                    "台帳データソースは必須です。"
            );
        }

        validateIdentifier(catalog.getPhysicalName(), "physicalName");

        if (selectedColumns.isEmpty()) {
            throw new IllegalArgumentException(
                    "台帳テンプレート変数が設定されていません。"
            );
        }

        Set<String> allowedColumns = catalog.getColumns().stream()
                .filter(column ->
                        column.isActiveFlag()
                                && column.getDeletedAt() == null
                )
                .map(column -> column.getColumnName())
                .collect(java.util.stream.Collectors.toSet());

        selectedColumns.forEach(column -> {
            validateIdentifier(column, "sourceColumn");
            if (!allowedColumns.contains(column)) {
                throw new IllegalArgumentException(
                        "許可されていない台帳データ項目です: "
                                + column
                );
            }
        });

        boolean filteredSelection = StringUtils.hasText(selectionColumn);
        if (filteredSelection) {
            validateIdentifier(selectionColumn, "selectionColumn");
            if (!allowedColumns.contains(selectionColumn)) {
                throw new IllegalArgumentException(
                        "許可されていない選択項目です: "
                                + selectionColumn
                );
            }
            if (selectionValues == null || selectionValues.isEmpty()) {
                throw new IllegalArgumentException(
                        "選択値は1件以上必要です。"
                );
            }
        }

        String whereClause = resolveWhereClause(catalog);
        List<String> orderColumns = catalog.getColumns().stream()
                .filter(column ->
                        column.isActiveFlag()
                                && column.getDeletedAt() == null
                                && selectedColumns.contains(
                                        column.getColumnName()
                                )
                )
                .sorted(java.util.Comparator
                        .comparingInt(column ->
                                column.getOrderNo() == null
                                        ? Integer.MAX_VALUE
                                        : column.getOrderNo()
                        ))
                .map(column -> column.getColumnName())
                .toList();

        int maxRows = catalog.getMaxRows();
        if (maxRows < 1 || maxRows > 10_000) {
            throw new IllegalStateException(
                    "台帳データソースのmaxRowsが不正です: "
                            + maxRows
            );
        }

        String selectionFilter = filteredSelection
                ? " AND " + selectionColumn + " IN (:selectionValues)"
                : "";
        String sql = """
                SELECT %s
                FROM %s
                WHERE %s%s
                ORDER BY %s
                LIMIT %d
                """.formatted(
                String.join(", ", selectedColumns),
                catalog.getPhysicalName(),
                whereClause,
                selectionFilter,
                String.join(", ", orderColumns),
                maxRows + 1
        );

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("tenantId", requireTenantId());
        parameters.put("targetMonth", targetMonth);
        if (filteredSelection) {
            parameters.put("selectionValues", selectionValues);
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql,
                parameters
        );

        if (rows.size() > maxRows) {
            throw new IllegalArgumentException(
                    "台帳データが上限を超えています。上限="
                            + maxRows
            );
        }

        return rows;
    }

    private String resolveWhereClause(
            ExcelBookDataSourceCatalog catalog
    ) {
        String whereClause = StringUtils.hasText(
                catalog.getWhereClauseTemplate()
        )
                ? catalog.getWhereClauseTemplate().trim()
                : catalog.isTenantScopedFlag()
                        ? "tenant_id = :tenantId"
                        : "1 = 1";

        String upper = whereClause.toUpperCase(Locale.ROOT);
        if (whereClause.contains(";")
                || whereClause.contains("--")
                || whereClause.contains("/*")
                || whereClause.contains("*/")
                || DANGEROUS_SQL.matcher(upper).find()) {
            throw new IllegalStateException(
                    "台帳データソースのWHERE句が不正です。"
            );
        }

        Matcher matcher = NAMED_PARAMETER.matcher(whereClause);
        while (matcher.find()) {
            if (!ALLOWED_PARAMETERS.contains(matcher.group(1))) {
                throw new IllegalStateException(
                        "台帳データソースで未許可のパラメータが使用されています: "
                                + matcher.group(1)
                );
            }
        }

        if (catalog.isTenantScopedFlag()
                && !whereClause.contains(":tenantId")) {
            throw new IllegalStateException(
                    "テナント対象データソースには:tenantIdが必要です。"
            );
        }

        return whereClause;
    }

    private void validateIdentifier(String value, String fieldName) {
        if (!StringUtils.hasText(value)
                || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " が不正です: " + value
            );
        }
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException(
                    "テナント情報を取得できません。"
            );
        }
        return tenantId;
    }
}
