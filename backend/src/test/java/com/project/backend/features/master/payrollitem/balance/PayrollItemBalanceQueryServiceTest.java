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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dailyreport.repository.DailyReportAllowanceRepository;
import com.project.backend.features.dailyreport.repository.DailyReportDeductionRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.transaction.EmployeePayrollItemTransactionRepository;

class PayrollItemBalanceQueryServiceTest {

    private PayrollItemBalancePolicyRepository policyRepository;
    private EmployeePayrollItemEnrollmentRepository enrollmentRepository;
    private DailyReportAllowanceRepository allowanceRepository;
    private DailyReportDeductionRepository deductionRepository;
    private EmployeePayrollItemTransactionRepository transactionRepository;
    private PayrollItemBalanceQueryService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("default");
        policyRepository = mock(PayrollItemBalancePolicyRepository.class);
        enrollmentRepository = mock(EmployeePayrollItemEnrollmentRepository.class);
        allowanceRepository = mock(DailyReportAllowanceRepository.class);
        deductionRepository = mock(DailyReportDeductionRepository.class);
        transactionRepository = mock(EmployeePayrollItemTransactionRepository.class);
        service = new PayrollItemBalanceQueryService(
                policyRepository, enrollmentRepository,
                allowanceRepository, deductionRepository, transactionRepository,
                new ObjectMapper()
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
        policy.setCarryForwardFlag(true);
        policy.setActiveFlag(true);

        EmployeePayrollItemEnrollment enrollment = new EmployeePayrollItemEnrollment();
        enrollment.setEmployeeId(10L);
        enrollment.setBalancePolicyId(5L);
        enrollment.setEffectiveFrom(LocalDate.of(2026, 7, 30));

        when(policyRepository
                .findByTenantIdAndTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "default", PayrollItemTargetType.DEDUCTION, 8L
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

    @Test
    void findDeductionBalance_shouldAccrueAmountFromDaysAndEnrollmentParameter() {
        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(5L);
        policy.setTargetType(PayrollItemTargetType.DEDUCTION);
        policy.setTargetMasterId(8L);
        policy.setTargetCode("GENERIC_DAILY_FEE");
        policy.setBalanceUnit(BalanceUnit.AMOUNT);
        policy.setAccrualRuleName(
                "CALENDAR_DAYS_TIMES_PARAMETER:dailyAmount");
        policy.setCarryForwardFlag(true);
        policy.setActiveFlag(true);

        EmployeePayrollItemEnrollment enrollment = new EmployeePayrollItemEnrollment();
        enrollment.setEmployeeId(10L);
        enrollment.setBalancePolicyId(5L);
        enrollment.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        enrollment.setSettingsJson("{\"dailyAmount\":\"1000\"}");

        when(policyRepository
                .findByTenantIdAndTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "default", PayrollItemTargetType.DEDUCTION, 8L
                )).thenReturn(Optional.of(policy));
        when(enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        10L, 5L, LocalDate.of(2026, 7, 31)
                )).thenReturn(List.of());
        when(enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        10L, 5L, LocalDate.of(2026, 8, 31)
                )).thenReturn(List.of(enrollment));
        when(deductionRepository.sumQuantity(
                "default", 10L, 8L, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 7, 31), null
        )).thenReturn(BigDecimal.ZERO);
        when(deductionRepository.sumQuantity(
                "default", 10L, 8L, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), null
        )).thenReturn(BigDecimal.valueOf(5000));

        PayrollItemBalanceSnapshot result = service.findDeductionBalance(
                10L, 8L, LocalDate.of(2026, 8, 10), null
        );

        assertThat(result.unit()).isEqualTo(BalanceUnit.AMOUNT);
        assertThat(result.accruedQuantity()).isEqualByComparingTo("31000");
        assertThat(result.consumedQuantity()).isEqualByComparingTo("5000");
        assertThat(result.remainingQuantity()).isEqualByComparingTo("26000");
    }

    @Test
    void findDeductionBalance_shouldBeUntrackedAfterEnrollmentIsDisabled() {
        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(5L);
        policy.setTargetType(PayrollItemTargetType.DEDUCTION);
        policy.setTargetMasterId(8L);
        policy.setBalanceUnit(BalanceUnit.DAYS);
        policy.setAccrualRuleName("CALENDAR_DAYS_IN_ENROLLMENT");
        policy.setActiveFlag(true);

        EmployeePayrollItemEnrollment enrollment = new EmployeePayrollItemEnrollment();
        enrollment.setEmployeeId(10L);
        enrollment.setBalancePolicyId(5L);
        enrollment.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        enrollment.setEffectiveTo(LocalDate.of(2026, 8, 9));

        when(policyRepository
                .findByTenantIdAndTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "default", PayrollItemTargetType.DEDUCTION, 8L
                )).thenReturn(Optional.of(policy));
        when(enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        10L, 5L, LocalDate.of(2026, 8, 31)
                )).thenReturn(List.of(enrollment));

        PayrollItemBalanceSnapshot result = service.findDeductionBalance(
                10L, 8L, LocalDate.of(2026, 8, 10), null
        );

        assertThat(result.tracked()).isFalse();
        assertThat(result.remainingQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void findAllowanceBalance_shouldUseAllowanceQuantities() {
        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(7L);
        policy.setTargetType(PayrollItemTargetType.ALLOWANCE);
        policy.setTargetMasterId(12L);
        policy.setBalanceUnit(BalanceUnit.COUNT);
        policy.setAccrualRuleName("CALENDAR_DAYS_IN_ENROLLMENT");
        policy.setActiveFlag(true);

        EmployeePayrollItemEnrollment enrollment = new EmployeePayrollItemEnrollment();
        enrollment.setEmployeeId(10L);
        enrollment.setBalancePolicyId(7L);
        enrollment.setEffectiveFrom(LocalDate.of(2026, 8, 1));

        when(policyRepository
                .findByTenantIdAndTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "default", PayrollItemTargetType.ALLOWANCE, 12L
                )).thenReturn(Optional.of(policy));
        when(enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        10L, 7L, LocalDate.of(2026, 8, 31)
                )).thenReturn(List.of(enrollment));
        when(allowanceRepository.sumQuantity(
                "default", 10L, 12L, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 7, 31), null
        )).thenReturn(BigDecimal.ZERO);
        when(allowanceRepository.sumQuantity(
                "default", 10L, 12L, LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), null
        )).thenReturn(BigDecimal.valueOf(6));
        when(transactionRepository.sumConfirmedQuantityByEffect(
                "default", 10L, "ALLOWANCE", 12L, "CREDIT",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null
        )).thenReturn(BigDecimal.TEN);
        when(transactionRepository.sumConfirmedQuantityByEffect(
                "default", 10L, "ALLOWANCE", 12L, "DEBIT",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), null
        )).thenReturn(BigDecimal.valueOf(2));

        PayrollItemBalanceSnapshot result = service.findAllowanceBalance(
                10L, 12L, LocalDate.of(2026, 8, 10), null
        );

        assertThat(result.tracked()).isTrue();
        assertThat(result.accruedQuantity()).isEqualByComparingTo("41");
        assertThat(result.consumedQuantity()).isEqualByComparingTo("8");
        assertThat(result.remainingQuantity()).isEqualByComparingTo("33");
    }

    @Test
    void findDeductionBalance_shouldCarryUnpaidAmountIntoTheNextMonth() {
        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(9L);
        policy.setTargetType(PayrollItemTargetType.DEDUCTION);
        policy.setTargetMasterId(15L);
        policy.setTargetCode("MOBILE_RENTAL");
        policy.setBalanceUnit(BalanceUnit.AMOUNT);
        policy.setAccrualRuleName("MANUAL_TRANSACTION");
        policy.setCarryForwardFlag(true);
        policy.setActiveFlag(true);

        EmployeePayrollItemEnrollment enrollment = new EmployeePayrollItemEnrollment();
        enrollment.setEmployeeId(10L);
        enrollment.setBalancePolicyId(9L);
        enrollment.setEffectiveFrom(LocalDate.of(2026, 7, 1));

        when(policyRepository
                .findByTenantIdAndTargetTypeAndTargetMasterIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "default", PayrollItemTargetType.DEDUCTION, 15L
                )).thenReturn(Optional.of(policy));
        when(enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        10L, 9L, LocalDate.of(2026, 7, 31)
                )).thenReturn(List.of(enrollment));
        when(enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        10L, 9L, LocalDate.of(2026, 8, 31)
                )).thenReturn(List.of(enrollment));
        when(deductionRepository.sumQuantity(
                "default", 10L, 15L, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31), null
        )).thenReturn(BigDecimal.valueOf(3000));
        when(deductionRepository.sumQuantity(
                "default", 10L, 15L, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31), null
        )).thenReturn(BigDecimal.valueOf(7000));
        when(transactionRepository.sumConfirmedQuantityByEffect(
                "default", 10L, "DEDUCTION", 15L, "CREDIT",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null
        )).thenReturn(BigDecimal.valueOf(10000));
        when(transactionRepository.sumConfirmedQuantityByEffect(
                "default", 10L, "DEDUCTION", 15L, "CREDIT",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), null
        )).thenReturn(BigDecimal.valueOf(15000));

        PayrollItemBalanceSnapshot result = service.findDeductionBalance(
                10L, 15L, LocalDate.of(2026, 8, 20), null
        );

        assertThat(result.openingQuantity()).isEqualByComparingTo("7000");
        assertThat(result.accruedQuantity()).isEqualByComparingTo("5000");
        assertThat(result.consumedQuantity()).isEqualByComparingTo("4000");
        assertThat(result.remainingQuantity()).isEqualByComparingTo("8000");

        policy.setCarryForwardFlag(false);
        PayrollItemBalanceSnapshot withoutCarry = service.findDeductionBalance(
                10L, 15L, LocalDate.of(2026, 8, 20), null
        );
        assertThat(withoutCarry.openingQuantity()).isEqualByComparingTo("0");
        assertThat(withoutCarry.remainingQuantity()).isEqualByComparingTo("1000");

        policy.setCarryForwardFlag(true);
        policy.setAdvanceConsumptionFlag(true);
        when(deductionRepository.sumQuantity(
                "default", 10L, 15L, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31), null
        )).thenReturn(BigDecimal.valueOf(20000));
        PayrollItemBalanceSnapshot overdrawn = service.findDeductionBalance(
                10L, 15L, LocalDate.of(2026, 8, 20), null
        );
        assertThat(overdrawn.advanceConsumptionAllowed()).isTrue();
        assertThat(overdrawn.remainingQuantity()).isEqualByComparingTo("-5000");
    }
}
