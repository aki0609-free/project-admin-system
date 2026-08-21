package com.project.backend.features.master.payrollitem.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.app.tenant.context.TenantContext;

class PayrollItemEnrollmentServiceTest {

    @Test
    void synchronize_shouldRecordEnableAndDisableDatesFromClock() {
        TenantContext.setTenantId("default");
        var policyRepository = mock(PayrollItemBalancePolicyRepository.class);
        var enrollmentRepository = mock(EmployeePayrollItemEnrollmentRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-09T03:00:00Z"), ZoneOffset.UTC);
        PayrollItemEnrollmentService service = new PayrollItemEnrollmentService(
                policyRepository, enrollmentRepository, clock,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mock(PayrollItemParameterDefinitionRepository.class)
        );

        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(5L);
        when(policyRepository.findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                "default", PayrollItemTargetType.DEDUCTION, "DORMITORY_FEE"
        )).thenReturn(Optional.of(policy));
        when(enrollmentRepository
                .findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                        10L, 5L
                )).thenReturn(Optional.empty());

        service.synchronize(
                10L, PayrollItemTargetType.DEDUCTION, "DORMITORY_FEE", true
        );

        var captor = org.mockito.ArgumentCaptor
                .forClass(EmployeePayrollItemEnrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getEffectiveFrom())
                .isEqualTo(LocalDate.of(2026, 8, 9));

        when(enrollmentRepository
                .findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                        10L, 5L
                )).thenReturn(Optional.of(captor.getValue()));
        service.synchronize(
                10L, PayrollItemTargetType.DEDUCTION, "DORMITORY_FEE", false
        );
        assertThat(captor.getValue().getEffectiveTo())
                .isEqualTo(LocalDate.of(2026, 8, 8));
        TenantContext.clear();
    }

    @Test
    void synchronize_shouldUseExplicitInitialEffectiveDate() {
        TenantContext.setTenantId("default");
        var policyRepository = mock(PayrollItemBalancePolicyRepository.class);
        var enrollmentRepository = mock(EmployeePayrollItemEnrollmentRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-15T03:00:00Z"), ZoneOffset.UTC);
        PayrollItemEnrollmentService service = new PayrollItemEnrollmentService(
                policyRepository, enrollmentRepository, clock,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mock(PayrollItemParameterDefinitionRepository.class)
        );

        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(5L);
        when(policyRepository.findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                "default", PayrollItemTargetType.DEDUCTION, "DORMITORY_FEE"
        )).thenReturn(Optional.of(policy));
        when(enrollmentRepository
                .findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                        10L, 5L
                )).thenReturn(Optional.empty());

        service.synchronize(
                10L,
                PayrollItemTargetType.DEDUCTION,
                "DORMITORY_FEE",
                true,
                java.util.Map.of(),
                LocalDate.of(2026, 4, 1)
        );

        var captor = org.mockito.ArgumentCaptor
                .forClass(EmployeePayrollItemEnrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getEffectiveFrom())
                .isEqualTo(LocalDate.of(2026, 4, 1));
        TenantContext.clear();
    }
}
