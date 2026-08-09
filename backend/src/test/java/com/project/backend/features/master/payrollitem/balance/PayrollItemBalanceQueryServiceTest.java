package com.project.backend.features.master.payrollitem.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dailyreport.repository.DailyReportDeductionRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

class PayrollItemBalanceQueryServiceTest {

    private PayrollItemBalancePolicyRepository policyRepository;
    private EmployeePayrollItemEnrollmentRepository enrollmentRepository;
    private DailyReportDeductionRepository deductionRepository;
    private PayrollItemBalanceQueryService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("default");
        policyRepository = mock(PayrollItemBalancePolicyRepository.class);
        enrollmentRepository = mock(EmployeePayrollItemEnrollmentRepository.class);
        deductionRepository = mock(DailyReportDeductionRepository.class);
        service = new PayrollItemBalanceQueryService(
                policyRepository, enrollmentRepository, deductionRepository
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findDeductionBalance_shouldCarryOnlyUnpaidQuantityWithoutDayAllocation() {
        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(5L);
        policy.setTargetType(PayrollItemTargetType.DEDUCTION);
        policy.setTargetMasterId(8L);
        policy.setTargetCode("DORMITORY_FEE");
        policy.setBalanceUnit(BalanceUnit.DAYS);
        policy.setAccrualRuleName("CALENDAR_DAYS_IN_ENROLLMENT");
        policy.setActiveFlag(true);

        EmployeePayrollItemEnrollment enrollment = new EmployeePayrollItemEnrollment();
        enrollment.setEmployeeId(10L);
        enrollment.setBalancePolicyId(5L);
        enrollment.setEffectiveFrom(LocalDate.of(2026, 7, 30));

        when(policyRepository
                .findByTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        PayrollItemTargetType.DEDUCTION, 8L
                )).thenReturn(Optional.of(policy));
        when(enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        10L, 5L, LocalDate.of(2026, 7, 31)
                )).thenReturn(List.of(enrollment));
        when(enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        10L, 5L, LocalDate.of(2026, 8, 31)
                )).thenReturn(List.of(enrollment));
        when(deductionRepository.sumQuantity(
                "default", 10L, 8L, LocalDate.of(2026, 7, 30),
                LocalDate.of(2026, 7, 31), 99L
        )).thenReturn(BigDecimal.ONE);
        when(deductionRepository.sumQuantity(
                "default", 10L, 8L, LocalDate.of(2026, 7, 30),
                LocalDate.of(2026, 8, 31), 99L
        )).thenReturn(BigDecimal.valueOf(5));

        PayrollItemBalanceSnapshot result = service.findDeductionBalance(
                10L, 8L, LocalDate.of(2026, 8, 10), 99L
        );

        assertThat(result.openingQuantity()).isEqualByComparingTo("1");
        assertThat(result.accruedQuantity()).isEqualByComparingTo("31");
        assertThat(result.consumedQuantity()).isEqualByComparingTo("4");
        assertThat(result.remainingQuantity()).isEqualByComparingTo("28");
    }
}
