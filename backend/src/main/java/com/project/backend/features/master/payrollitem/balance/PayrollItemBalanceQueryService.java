package com.project.backend.features.master.payrollitem.balance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dailyreport.repository.DailyReportAllowanceRepository;
import com.project.backend.features.dailyreport.repository.DailyReportDeductionRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.transaction.EmployeePayrollItemTransactionRepository;
import com.project.backend.features.master.payrollitem.transaction.PayrollItemBalanceEffect;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollItemBalanceQueryService {

    private static final String CALENDAR_DAYS_IN_ENROLLMENT =
            "CALENDAR_DAYS_IN_ENROLLMENT";
    private static final String CALENDAR_DAYS_TIMES_PARAMETER_PREFIX =
            "CALENDAR_DAYS_TIMES_PARAMETER:";

    private final PayrollItemBalancePolicyRepository policyRepository;
    private final EmployeePayrollItemEnrollmentRepository enrollmentRepository;
    private final DailyReportAllowanceRepository allowanceRepository;
    private final DailyReportDeductionRepository deductionRepository;
    private final EmployeePayrollItemTransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    public Optional<Long> findPolicyMasterId(
            PayrollItemTargetType targetType,
            String targetCode
    ) {
        return policyRepository
                .findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                        TenantContext.getTenantId(), targetType, targetCode)
                .filter(PayrollItemBalancePolicy::isActiveFlag)
                .map(PayrollItemBalancePolicy::getTargetMasterId);
    }

    public boolean isManagedDeduction(Long deductionMasterId) {
        return deductionMasterId != null && policyRepository
                .findByTenantIdAndTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        TenantContext.getTenantId(),
                        PayrollItemTargetType.DEDUCTION,
                        deductionMasterId)
                .isPresent();
    }

    public PayrollItemBalanceSnapshot findDeductionBalance(
            Long employeeId,
            Long deductionMasterId,
            LocalDate targetDate,
            Long excludeDailyReportId
    ) {
        return findDeductionBalance(employeeId, deductionMasterId, targetDate,
                excludeDailyReportId, null);
    }

    public PayrollItemBalanceSnapshot findDeductionBalance(
            Long employeeId,
            Long deductionMasterId,
            LocalDate targetDate,
            Long excludeDailyReportId,
            Long excludeTransactionId
    ) {
        return findBalance(
                PayrollItemTargetType.DEDUCTION,
                employeeId,
                deductionMasterId,
                targetDate,
                excludeDailyReportId,
                excludeTransactionId
        );
    }

    public PayrollItemBalanceSnapshot findAllowanceBalance(
            Long employeeId,
            Long allowanceMasterId,
            LocalDate targetDate,
            Long excludeDailyReportId
    ) {
        return findAllowanceBalance(employeeId, allowanceMasterId, targetDate,
                excludeDailyReportId, null);
    }

    public PayrollItemBalanceSnapshot findAllowanceBalance(
            Long employeeId,
            Long allowanceMasterId,
            LocalDate targetDate,
            Long excludeDailyReportId,
            Long excludeTransactionId
    ) {
        return findBalance(
                PayrollItemTargetType.ALLOWANCE,
                employeeId,
                allowanceMasterId,
                targetDate,
                excludeDailyReportId,
                excludeTransactionId
        );
    }

    private PayrollItemBalanceSnapshot findBalance(
            PayrollItemTargetType targetType,
            Long employeeId,
            Long targetMasterId,
            LocalDate targetDate,
            Long excludeDailyReportId,
            Long excludeTransactionId
    ) {
        if (employeeId == null || targetMasterId == null || targetDate == null) {
            return PayrollItemBalanceSnapshot.untracked();
        }

        var policy = policyRepository
                .findByTenantIdAndTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        TenantContext.getTenantId(),
                        targetType,
                        targetMasterId
                )
                .orElse(null);
        if (policy == null) {
            return PayrollItemBalanceSnapshot.untracked();
        }
        if (!policy.isBalanceTrackingFlag()) {
            return PayrollItemBalanceSnapshot.untracked();
        }
        String accrualRuleName = policy.getAccrualRuleName();
        if (!CALENDAR_DAYS_IN_ENROLLMENT.equals(accrualRuleName)
                && !"MANUAL_TRANSACTION".equals(accrualRuleName)
                && (accrualRuleName == null || !accrualRuleName.startsWith(
                        CALENDAR_DAYS_TIMES_PARAMETER_PREFIX))) {
            throw new IllegalStateException(
                    "未対応の残高加算Ruleです。rule=" + policy.getAccrualRuleName()
            );
        }

        LocalDate monthStart = targetDate.withDayOfMonth(1);
        LocalDate monthEnd = targetDate.withDayOfMonth(targetDate.lengthOfMonth());
        var enrollmentsThroughMonthEnd = enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        employeeId, policy.getId(), monthEnd
                );
        boolean activeOnTargetDate = enrollmentsThroughMonthEnd.stream()
                .anyMatch(enrollment -> !enrollment.getEffectiveFrom().isAfter(targetDate)
                        && (enrollment.getEffectiveTo() == null
                        || !enrollment.getEffectiveTo().isBefore(targetDate)));
        if (!activeOnTargetDate) {
            return PayrollItemBalanceSnapshot.untracked();
        }
        LocalDate enrollmentStart = enrollmentsThroughMonthEnd.stream()
                .map(EmployeePayrollItemEnrollment::getEffectiveFrom)
                .min(LocalDate::compareTo)
                .orElse(null);
        if (enrollmentStart == null) {
            return PayrollItemBalanceSnapshot.untracked();
        }
        BigDecimal accruedThroughPreviousMonth = accrued(
                employeeId, policy, monthStart.minusDays(1)
        );
        BigDecimal accruedThroughTargetMonth = accrued(
                employeeId, policy, monthEnd
        );

        String tenantId = TenantContext.getTenantId();
        BigDecimal consumedBeforeMonth = sumQuantity(
                targetType, tenantId, employeeId, targetMasterId,
                enrollmentStart, monthStart.minusDays(1), excludeDailyReportId);
        BigDecimal consumedThroughMonth = sumQuantity(
                targetType, tenantId, employeeId, targetMasterId,
                enrollmentStart, monthEnd, excludeDailyReportId);

        BigDecimal creditedBeforeMonth = sumTransactionQuantity(
                targetType, employeeId, targetMasterId,
                PayrollItemBalanceEffect.CREDIT,
                enrollmentStart, monthStart.minusDays(1), excludeTransactionId);
        BigDecimal creditedThroughMonth = sumTransactionQuantity(
                targetType, employeeId, targetMasterId,
                PayrollItemBalanceEffect.CREDIT,
                enrollmentStart, monthEnd, excludeTransactionId);
        BigDecimal debitedBeforeMonth = sumTransactionQuantity(
                targetType, employeeId, targetMasterId,
                PayrollItemBalanceEffect.DEBIT,
                enrollmentStart, monthStart.minusDays(1), excludeTransactionId);
        BigDecimal debitedThroughMonth = sumTransactionQuantity(
                targetType, employeeId, targetMasterId,
                PayrollItemBalanceEffect.DEBIT,
                enrollmentStart, monthEnd, excludeTransactionId);

        BigDecimal calculatedOpening = accruedThroughPreviousMonth
                .add(creditedBeforeMonth)
                .subtract(consumedBeforeMonth)
                .subtract(debitedBeforeMonth);
        BigDecimal opening = policy.isCarryForwardFlag()
                ? calculatedOpening : BigDecimal.ZERO;
        BigDecimal currentAccrual = accruedThroughTargetMonth
                .subtract(accruedThroughPreviousMonth)
                .add(creditedThroughMonth.subtract(creditedBeforeMonth));
        BigDecimal currentConsumed = consumedThroughMonth
                .subtract(consumedBeforeMonth)
                .add(debitedThroughMonth.subtract(debitedBeforeMonth));
        BigDecimal remaining = opening.add(currentAccrual).subtract(currentConsumed);
        if (!policy.isAdvanceConsumptionFlag()) {
            opening = opening.max(BigDecimal.ZERO);
            remaining = remaining.max(BigDecimal.ZERO);
        }

        return new PayrollItemBalanceSnapshot(
                true,
                policy.getBalanceUnit(),
                policy.isAdvanceConsumptionFlag(),
                opening,
                currentAccrual,
                currentConsumed,
                remaining
        );
    }

    private BigDecimal sumQuantity(
            PayrollItemTargetType targetType,
            String tenantId,
            Long employeeId,
            Long masterId,
            LocalDate from,
            LocalDate through,
            Long excludeDailyReportId
    ) {
        return switch (targetType) {
            case ALLOWANCE -> allowanceRepository.sumQuantity(
                    tenantId, employeeId, masterId, from, through,
                    excludeDailyReportId);
            case DEDUCTION -> deductionRepository.sumQuantity(
                    tenantId, employeeId, masterId, from, through,
                    excludeDailyReportId);
        };
    }

    private BigDecimal accrued(
            Long employeeId,
            PayrollItemBalancePolicy policy,
            LocalDate through
    ) {
        if (through == null) {
            return BigDecimal.ZERO;
        }
        if ("MANUAL_TRANSACTION".equals(policy.getAccrualRuleName())) {
            return BigDecimal.ZERO;
        }
        var enrollments = enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        employeeId, policy.getId(), through
                );
        if (policy.getAccrualRuleName().startsWith(
                CALENDAR_DAYS_TIMES_PARAMETER_PREFIX)) {
            String parameterKey = policy.getAccrualRuleName().substring(
                    CALENDAR_DAYS_TIMES_PARAMETER_PREFIX.length());
            if (parameterKey.isBlank()) {
                throw new IllegalStateException(
                        "残高加算単価のパラメーターキーが未設定です。"
                );
            }
            return enrollments.stream()
                    .map(enrollment -> BigDecimal.valueOf(
                                    overlapDays(enrollment, through))
                            .multiply(parameterAmount(
                                    enrollment, parameterKey)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        long days = enrollments.stream()
                .mapToLong(enrollment -> overlapDays(enrollment, through))
                .sum();
        return BigDecimal.valueOf(days);
    }

    private BigDecimal parameterAmount(
            EmployeePayrollItemEnrollment enrollment,
            String parameterKey
    ) {
        Map<String, String> settings;
        try {
            settings = enrollment.getSettingsJson() == null
                    || enrollment.getSettingsJson().isBlank()
                    ? Map.of()
                    : objectMapper.readValue(
                            enrollment.getSettingsJson(),
                            new TypeReference<Map<String, String>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "従業員別手当・控除設定を読み込めません。", exception
            );
        }
        String value = settings.get(parameterKey);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "残高加算単価が設定されていません。key=" + parameterKey
            );
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "残高加算単価が数値ではありません。key=" + parameterKey,
                    exception
            );
        }
    }

    private BigDecimal sumTransactionQuantity(
            PayrollItemTargetType targetType,
            Long employeeId,
            Long masterId,
            PayrollItemBalanceEffect effect,
            LocalDate from,
            LocalDate through,
            Long excludeTransactionId
    ) {
        if (through.isBefore(from)) {
            return BigDecimal.ZERO;
        }
        return Optional.ofNullable(
                transactionRepository.sumConfirmedQuantityByEffect(
                        TenantContext.getTenantId(), employeeId,
                        targetType.name(), masterId, effect.name(), from,
                        through, excludeTransactionId
                )
        ).orElse(BigDecimal.ZERO);
    }

    private long overlapDays(
            EmployeePayrollItemEnrollment enrollment,
            LocalDate through
    ) {
        LocalDate end = enrollment.getEffectiveTo() == null
                || enrollment.getEffectiveTo().isAfter(through)
                ? through
                : enrollment.getEffectiveTo();
        if (end.isBefore(enrollment.getEffectiveFrom())) {
            return 0;
        }
        return ChronoUnit.DAYS.between(enrollment.getEffectiveFrom(), end) + 1;
    }
}
