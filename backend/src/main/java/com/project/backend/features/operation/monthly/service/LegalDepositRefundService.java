package com.project.backend.features.operation.monthly.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.entity.LegalDepositRefund;
import com.project.backend.features.operation.monthly.enums.LegalDepositRefundStatus;
import com.project.backend.features.operation.monthly.repository.LegalDepositRefundRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LegalDepositRefundService {

    private static final String BALANCE_SQL = """
            SELECT deposit.employee_id,
                   deposit.deposit_amount - COALESCE(refund.refund_amount, 0) AS balance
            FROM (
                SELECT dr.employee_id, SUM(drd.amount) AS deposit_amount
                FROM daily_report dr
                JOIN daily_report_deductions drd
                  ON drd.tenant_id = dr.tenant_id
                 AND drd.daily_report_id = dr.id
                 AND drd.deleted_at IS NULL
                 AND drd.deduction_code = 'LEGAL_DEPOSIT'
                WHERE dr.tenant_id = ?
                  AND dr.deleted_at IS NULL
                  AND dr.approval_status = 'APPROVED'
                  AND dr.work_date <= ?
                GROUP BY dr.employee_id
            ) deposit
            LEFT JOIN (
                SELECT employee_id, SUM(amount) AS refund_amount
                FROM employee_legal_deposit_refund
                WHERE tenant_id = ?
                  AND deleted_at IS NULL
                  AND status = 'ACTIVE'
                  AND period_end <= ?
                  AND monthly_closing_id <> ?
                GROUP BY employee_id
            ) refund ON refund.employee_id = deposit.employee_id
            WHERE deposit.deposit_amount - COALESCE(refund.refund_amount, 0) > 0
            ORDER BY deposit.employee_id
            """;

    private final LegalDepositRefundRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public List<LegalDepositRefund> prepareRefunds(
            Long monthlyClosingId,
            MonthlyClosingPeriod period,
            Integer closingVersion
    ) {
        if (monthlyClosingId == null || period == null
                || closingVersion == null || closingVersion < 1) {
            throw new IllegalArgumentException("法定預り返金の締め情報が不正です。");
        }

        supersedeCurrentRefunds(monthlyClosingId);

        String tenantId = TenantContext.getTenantId();
        List<RefundBalance> balances = jdbcTemplate.query(
                BALANCE_SQL,
                (resultSet, rowNumber) -> new RefundBalance(
                        resultSet.getLong("employee_id"),
                        resultSet.getBigDecimal("balance")
                ),
                tenantId,
                period.endDate(),
                tenantId,
                period.endDate(),
                monthlyClosingId
        );

        return repository.saveAll(balances.stream()
                .map(balance -> newRefund(
                        monthlyClosingId, period, closingVersion, balance
                ))
                .toList());
    }

    private void supersedeCurrentRefunds(Long monthlyClosingId) {
        Instant now = Instant.now(clock);
        repository.findByMonthlyClosingIdAndStatusAndDeletedAtIsNull(
                        monthlyClosingId,
                        LegalDepositRefundStatus.ACTIVE
                )
                .forEach(refund -> {
                    refund.setStatus(LegalDepositRefundStatus.SUPERSEDED);
                    refund.setSupersededAt(now);
                });
    }

    private LegalDepositRefund newRefund(
            Long monthlyClosingId,
            MonthlyClosingPeriod period,
            Integer closingVersion,
            RefundBalance balance
    ) {
        LegalDepositRefund refund = new LegalDepositRefund();
        refund.setMonthlyClosingId(monthlyClosingId);
        refund.setTargetMonth(java.time.YearMonth
                .parse(period.targetMonth()).atDay(1));
        refund.setPeriodEnd(period.endDate());
        refund.setClosingVersion(closingVersion);
        refund.setEmployeeId(balance.employeeId());
        refund.setAmount(balance.amount());
        refund.setStatus(LegalDepositRefundStatus.ACTIVE);
        return refund;
    }

    private record RefundBalance(Long employeeId, BigDecimal amount) {
    }
}
