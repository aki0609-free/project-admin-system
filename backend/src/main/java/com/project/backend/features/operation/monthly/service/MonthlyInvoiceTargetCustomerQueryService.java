package com.project.backend.features.operation.monthly.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

/**
 * 対象期間に請求明細が存在する顧客だけを月次請求対象として取得する。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyInvoiceTargetCustomerQueryService {

    private static final String TARGET_CUSTOMER_SQL = """
            SELECT DISTINCT source.customer_id
            FROM vw_monthly_invoice_latest_detail source
            WHERE source.tenant_id = ?
              AND source.work_date BETWEEN ? AND ?
            ORDER BY source.customer_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public List<Long> findTargetCustomerIds(
            LocalDate periodFrom,
            LocalDate periodTo
    ) {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException(
                    "月次請求対象の取得に必要なtenantIdがありません。"
            );
        }
        return jdbcTemplate.queryForList(
                TARGET_CUSTOMER_SQL,
                Long.class,
                tenantId,
                periodFrom,
                periodTo
        );
    }
}
