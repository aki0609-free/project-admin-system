package com.project.backend.features.operation.monthly.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.common.dayrule.utils.DayRuleUtils;
import com.project.backend.features.operation.monthly.dto.CustomerBillingClosingResponse;
import com.project.backend.features.operation.monthly.dto.CustomerBillingSummaryResponse;
import com.project.backend.features.operation.monthly.dto.CustomerBillingTargetResponse;
import com.project.backend.features.operation.monthly.entity.CustomerBillingClosing;
import com.project.backend.features.operation.monthly.repository.CustomerBillingClosingRepository;
import com.project.backend.features.operation.monthly.service.CustomerBillingTargetService.Target;
import com.project.backend.features.operation.monthly.utils.MonthlyOperationDateUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerBillingSummaryService {

    private static final BigDecimal V1_TAX_RATE = new BigDecimal("0.10");
    private static final String AMOUNT_SQL = """
            WITH scoped AS (
                SELECT
                    report.billing_rate_id,
                    report.billing_unit,
                    COALESCE(report.work_hours, 0) AS work_hours,
                    COALESCE(report.billing_base_unit_price, 0) AS base_unit_price,
                    COALESCE(report.overtime_hours, 0) AS overtime_hours,
                    COALESCE(report.billing_overtime_unit_price, 0) AS overtime_unit_price,
                    COALESCE(report.night_work_hours, 0) AS night_hours,
                    COALESCE(report.billing_night_unit_price, 0) AS night_unit_price,
                    COALESCE(report.holiday_work_hours, 0) AS holiday_hours,
                    COALESCE(report.billing_holiday_unit_price, 0) AS holiday_unit_price,
                    COALESCE(report.mileage, 0) AS mileage,
                    COALESCE(report.billing_commute_unit_price, 0) AS commute_unit_price,
                    ROW_NUMBER() OVER (
                        PARTITION BY report.billing_rate_id
                        ORDER BY report.work_date, report.id
                    ) AS billing_rate_occurrence
                FROM daily_report report
                WHERE report.tenant_id = ?
                  AND report.customer_id = ?
                  AND report.work_date BETWEEN ? AND ?
                  AND report.deleted_at IS NULL
                  AND report.approval_status = 'APPROVED'
            )
            SELECT
                COALESCE(SUM(
                    ROUND(
                        CASE
                            WHEN scoped.billing_unit = 'DAILY'
                                THEN scoped.work_hours / 8 * scoped.base_unit_price
                            WHEN scoped.billing_unit = 'HOURLY'
                                THEN scoped.work_hours * scoped.base_unit_price
                            WHEN scoped.billing_unit = 'MONTHLY'
                                 AND scoped.billing_rate_occurrence = 1
                                THEN scoped.base_unit_price
                            ELSE 0
                        END,
                        0
                    )
                    + ROUND(scoped.overtime_hours * scoped.overtime_unit_price, 0)
                    + ROUND(scoped.night_hours * scoped.night_unit_price, 0)
                    + ROUND(scoped.holiday_hours * scoped.holiday_unit_price, 0)
                    + ROUND(scoped.mileage * scoped.commute_unit_price, 0)
                ), 0) AS subtotal_amount,
                COALESCE(SUM(
                    CASE
                        WHEN scoped.billing_unit NOT IN ('DAILY', 'HOURLY', 'MONTHLY')
                          OR (scoped.billing_unit = 'MONTHLY'
                              AND scoped.billing_rate_id IS NULL)
                            THEN 1
                        ELSE 0
                    END
                ), 0)
                    AS not_ready_count
            FROM scoped
            """;

    private final CustomerBillingClosingRepository repository;
    private final CustomerBillingTargetService targetService;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public CustomerBillingSummaryResponse findSummary(String targetMonthText) {
        YearMonth targetMonth =
                MonthlyOperationDateUtil.parseTargetMonth(targetMonthText);
        var targets = targetService.findTargets(targetMonthText);
        List<Long> customerIds = targets.stream()
                .map(target -> target.customer().getId())
                .toList();
        Map<Long, CustomerBillingClosing> closingByCustomerId = customerIds.isEmpty()
                ? Map.of()
                : repository
                        .findAllByTargetMonthAndCustomerIdInAndDeletedAtIsNull(
                                targetMonth.atDay(1),
                                customerIds
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                CustomerBillingClosing::getCustomerId,
                                Function.identity()
                        ));
        LocalDate today = LocalDate.now(clock);
        List<CustomerBillingTargetResponse> customers = targets
                .stream()
                .map(target -> toTargetResponse(
                        target,
                        closingByCustomerId.get(target.customer().getId()),
                        today
                ))
                .toList();
        int closedCount = (int) customers.stream()
                .filter(customer -> customer.closing() != null
                        && "CLOSED".equals(customer.closing().status()))
                .count();
        int eligibleCount = (int) customers.stream()
                .filter(CustomerBillingTargetResponse::closingDateReached)
                .count();
        String status = customers.isEmpty()
                ? "TARGET_NONE"
                : closedCount == 0
                        ? "OPEN"
                        : closedCount == customers.size()
                                ? "CLOSED"
                                : "PARTIALLY_CLOSED";
        return new CustomerBillingSummaryResponse(
                targetMonthText,
                status,
                customers.size(),
                eligibleCount,
                closedCount,
                customers
        );
    }

    private CustomerBillingTargetResponse toTargetResponse(
            Target target,
            CustomerBillingClosing closing,
            LocalDate today
    ) {
        Amount amount = jdbcTemplate.queryForObject(
                AMOUNT_SQL,
                (rs, rowNum) -> new Amount(
                        rs.getBigDecimal("subtotal_amount"),
                        rs.getLong("not_ready_count")
                ),
                TenantContext.getTenantId(),
                target.customer().getId(),
                target.period().startDate(),
                target.period().endDate()
        );
        BigDecimal subtotal = amount == null || amount.subtotal() == null
                ? BigDecimal.ZERO
                : amount.subtotal();
        BigDecimal tax = subtotal.multiply(V1_TAX_RATE)
                .setScale(0, RoundingMode.HALF_UP);
        return new CustomerBillingTargetResponse(
                target.customer().getId(),
                target.customer().getName(),
                DayRuleUtils.toLabel(
                        target.period().rule().type(),
                        target.period().rule().value(),
                        target.period().rule().monthOffset()
                ),
                target.period().startDate(),
                target.period().endDate(),
                subtotal,
                tax,
                subtotal.add(tax),
                amount != null && amount.notReadyCount() == 0,
                !today.isBefore(target.period().endDate()),
                closing == null ? null : toResponse(closing)
        );
    }

    private CustomerBillingClosingResponse toResponse(
            CustomerBillingClosing closing
    ) {
        return new CustomerBillingClosingResponse(
                closing.getId(),
                closing.getTargetMonth(),
                closing.getCustomerId(),
                closing.getStatus().name(),
                closing.getClosingVersion(),
                closing.getClosedAt()
        );
    }

    private record Amount(BigDecimal subtotal, long notReadyCount) {
    }
}
