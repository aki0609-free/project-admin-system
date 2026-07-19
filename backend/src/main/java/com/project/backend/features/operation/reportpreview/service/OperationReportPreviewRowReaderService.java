package com.project.backend.features.operation.reportpreview.service;

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

        if (request.operationType() == OperationType.MONTHLY) {
            return jdbcTemplate.queryForList("""
                    select *
                    from %s
                    where tenant_id = :tenantId
                      and target_month = :targetMonth
                    %s
                    """.formatted(tableName, orderBySql),
                    Map.of(
                            "tenantId", tenantId,
                            "targetMonth", request.targetMonth()
                    ));
        }

        if (request.operationType() == OperationType.DAILY) {
            return jdbcTemplate.queryForList("""
                    select *
                    from %s
                    where tenant_id = :tenantId
                      and payment_date = :targetDate
                    %s
                    """.formatted(tableName, orderBySql),
                    Map.of(
                            "tenantId", tenantId,
                            "targetDate", request.targetDate()
                    ));
        }

        return jdbcTemplate.queryForList("""
                select *
                from %s
                where tenant_id = :tenantId
                  and target_date = :targetDate
                %s
                """.formatted(tableName, orderBySql),
                Map.of(
                        "tenantId", tenantId,
                        "targetDate", request.targetDate()
                ));
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