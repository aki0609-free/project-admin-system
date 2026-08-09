package com.project.backend.features.master.payrollitem.balance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dailyreport.repository.DailyReportDeductionRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollItemBalanceQueryService {

    private static final String CALENDAR_DAYS_IN_ENROLLMENT =
            "CALENDAR_DAYS_IN_ENROLLMENT";

    private final PayrollItemBalancePolicyRepository policyRepository;
    private final EmployeePayrollItemEnrollmentRepository enrollmentRepository;
    private final DailyReportDeductionRepository deductionRepository;

    public Optional<Long> findPolicyMasterId(
            PayrollItemTargetType targetType,
            String targetCode
    ) {
        return policyRepository
                .findByTargetTypeAndTargetCodeAndDeletedAtIsNull(targetType, targetCode)
                .filter(PayrollItemBalancePolicy::isActiveFlag)
                .map(PayrollItemBalancePolicy::getTargetMasterId);
    }

    public boolean isManagedDeduction(Long deductionMasterId) {
        return deductionMasterId != null && policyRepository
                .findByTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        PayrollItemTargetType.DEDUCTION, deductionMasterId)
                .isPresent();
    }

    public PayrollItemBalanceSnapshot findDeductionBalance(
            Long employeeId,
            Long deductionMasterId,
            LocalDate targetDate,
            Long excludeDailyReportId
    ) {
        if (employeeId == null || deductionMasterId == null || targetDate == null) {
            return PayrollItemBalanceSnapshot.untracked();
        }

        var policy = policyRepository
                .findByTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        PayrollItemTargetType.DEDUCTION,
                        deductionMasterId
                )
                .orElse(null);
        if (policy == null) {
            return PayrollItemBalanceSnapshot.untracked();
        }
        if (!CALENDAR_DAYS_IN_ENROLLMENT.equals(policy.getAccrualRuleName())) {
            throw new IllegalStateException(
                    "未対応の残高加算Ruleです。rule=" + policy.getAccrualRuleName()
            );
        }

        LocalDate monthStart = targetDate.withDayOfMonth(1);
        LocalDate monthEnd = targetDate.withDayOfMonth(targetDate.lengthOfMonth());
        LocalDate enrollmentStart = enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        employeeId, policy.getId(), monthEnd
                )
                .stream()
                .map(EmployeePayrollItemEnrollment::getEffectiveFrom)
                .min(LocalDate::compareTo)
                .orElse(null);
        if (enrollmentStart == null) {
            return PayrollItemBalanceSnapshot.untracked();
        }
        BigDecimal accruedThroughPreviousMonth = accrued(
                employeeId, policy.getId(), monthStart.minusDays(1)
        );
        BigDecimal accruedThroughTargetMonth = accrued(
                employeeId, policy.getId(), monthEnd
        );

        String tenantId = TenantContext.getTenantId();
        BigDecimal consumedBeforeMonth = deductionRepository.sumQuantity(
                tenantId,
                employeeId,
                deductionMasterId,
                enrollmentStart,
                monthStart.minusDays(1),
                excludeDailyReportId
        );
        BigDecimal consumedThroughMonth = deductionRepository.sumQuantity(
                tenantId,
                employeeId,
                deductionMasterId,
                enrollmentStart,
                monthEnd,
                excludeDailyReportId
        );

        BigDecimal opening = accruedThroughPreviousMonth.subtract(consumedBeforeMonth);
        BigDecimal currentAccrual = accruedThroughTargetMonth
                .subtract(accruedThroughPreviousMonth);
        BigDecimal currentConsumed = consumedThroughMonth.subtract(consumedBeforeMonth);
        BigDecimal remaining = opening.add(currentAccrual).subtract(currentConsumed);

        return new PayrollItemBalanceSnapshot(
                true,
                policy.getBalanceUnit(),
                opening.max(BigDecimal.ZERO),
                currentAccrual,
                currentConsumed,
                remaining.max(BigDecimal.ZERO)
        );
    }

    private BigDecimal accrued(
            Long employeeId,
            Long policyId,
            LocalDate through
    ) {
        if (through == null) {
            return BigDecimal.ZERO;
        }
        long days = enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        employeeId, policyId, through
                )
                .stream()
                .mapToLong(enrollment -> overlapDays(enrollment, through))
                .sum();
        return BigDecimal.valueOf(days);
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
