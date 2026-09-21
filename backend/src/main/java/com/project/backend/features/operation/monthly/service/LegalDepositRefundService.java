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

    private static final String PERIOD_REFUND_SQL = """
            SELECT employee_id, refund_amount
            FROM vw_monthly_pay_slip_legal_deposit_refund
            WHERE tenant_id = ?
              AND target_month = ?
            ORDER BY employee_id
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
            throw new IllegalArgumentException("法定準備金返金の締め情報が不正です。");
        }

        supersedeCurrentRefunds(monthlyClosingId);

        String tenantId = TenantContext.getTenantId();
        List<RefundBalance> balances = jdbcTemplate.query(
                PERIOD_REFUND_SQL,
                (resultSet, rowNumber) -> new RefundBalance(
                        resultSet.getLong("employee_id"),
                        resultSet.getBigDecimal("refund_amount")
                ),
                tenantId,
                java.time.YearMonth.parse(period.targetMonth()).atDay(1)
        );

        // JdbcTemplateで実行される後続の月次帳票処理から、旧ACTIVE行の
        // SUPERSEDED化と新しいACTIVE行を同じ状態で参照できるようにする。
        // saveAllだけではJPAの変更が帳票SQL実行時点までflushされず、
        // 再締め時に旧版と新版の返金額が二重集計されることがある。
        return repository.saveAllAndFlush(balances.stream()
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
