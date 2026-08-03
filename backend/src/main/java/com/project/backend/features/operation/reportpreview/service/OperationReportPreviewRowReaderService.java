package com.project.backend.features.operation.reportpreview.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.operation.reportpreview.dto.OperationReportPreviewHtmlRequest;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OperationReportPreviewRowReaderService {

    private static final Pattern SAFE_TABLE_NAME =
            Pattern.compile("^[A-Za-z0-9_]+$");

    private static final Pattern SAFE_COLUMN_NAME =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,99}$");

    private static final Pattern SAFE_ORDER_BY =
            Pattern.compile("^[A-Za-z0-9_,\\s]+$");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @SuppressWarnings("null")
    public List<Map<String, Object>> readRows(
            OperationReportPreview preview,
            OperationReportPreviewHtmlRequest request,
            String tenantId) {

        String tableName = preview.getTableName();

        validateTableName(tableName);

        String orderBySql = buildOrderBySql(preview.getOrderBy());
        String filterColumnName = resolveFilterColumnName(preview);

        if (request.operationType() == OperationType.MONTHLY) {
            String targetMonth = validateTargetMonth(
                    request.targetMonth()
            );
            return jdbcTemplate.queryForList("""
                    select *
                    from %s
                    where tenant_id = :tenantId
                      and %s = :targetMonth
                    %s
                    """.formatted(
                            tableName,
                            filterColumnName,
                            orderBySql
                    ),
                    Map.of(
                            "tenantId", tenantId,
                            "targetMonth", targetMonth
                    ));
        }

        if (request.operationType() == OperationType.DAILY) {
            LocalDate targetDate = validateTargetDate(
                    request.targetDate()
            );
            return jdbcTemplate.queryForList("""
                    select *
                    from %s
                    where tenant_id = :tenantId
                      and %s = :targetDate
                    %s
                    """.formatted(
                            tableName,
                            filterColumnName,
                            orderBySql
                    ),
                    Map.of(
                            "tenantId", tenantId,
                            "targetDate", targetDate
                    ));
        }

        LocalDate targetDate = validateTargetDate(
                request.targetDate()
        );

        return jdbcTemplate.queryForList("""
                select *
                from %s
                where tenant_id = :tenantId
                  and %s = :targetDate
                %s
                """.formatted(
                        tableName,
                        filterColumnName,
                        orderBySql
                ),
                Map.of(
                        "tenantId", tenantId,
                        "targetDate", targetDate
                ));
    }

    private LocalDate validateTargetDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "targetDateはyyyy-MM-dd形式で指定してください。",
                    e
            );
        }
    }

    private String validateTargetMonth(String value) {
        try {
            return YearMonth.parse(value).toString();
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "targetMonthはyyyy-MM形式で指定してください。",
                    e
            );
        }
    }

    private String resolveFilterColumnName(
            OperationReportPreview preview
    ) {
        String columnName = preview.getFilterColumnName();
        if (!StringUtils.hasText(columnName)) {
            columnName = preview.getOperationType() == OperationType.MONTHLY
                    ? "target_month"
                    : preview.getOperationType() == OperationType.DAILY
                            ? "payment_date"
                            : "target_date";
        }

        if (!SAFE_COLUMN_NAME.matcher(columnName).matches()) {
            throw new RuntimeException(
                    "不正なfilter_column_nameです: " + columnName
            );
        }
        return columnName;
    }

    private String buildOrderBySql(String orderBy) {
        if (!StringUtils.hasText(orderBy)) {
            return "";
        }

        if (!SAFE_ORDER_BY.matcher(orderBy).matches()) {
            throw new RuntimeException("不正な order_by です: " + orderBy);
        }

        return "order by " + orderBy;
    }

    private void validateTableName(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            throw new RuntimeException("tableName は必須です。");
        }

        if (!SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new RuntimeException("不正なテーブル名です: " + tableName);
        }
    }
}
