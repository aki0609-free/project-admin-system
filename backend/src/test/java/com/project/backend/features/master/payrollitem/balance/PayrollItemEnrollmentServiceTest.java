package com.project.backend.features.master.payrollitem.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.parameter.PayrollItemRuleParameterResolverRegistry;
import com.project.backend.app.tenant.context.TenantContext;

class PayrollItemEnrollmentServiceTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void synchronize_shouldRecordEnableAndDisableDatesFromClock() {
        TenantContext.setTenantId("default");
        var policyRepository = mock(PayrollItemBalancePolicyRepository.class);
        var enrollmentRepository = mock(EmployeePayrollItemEnrollmentRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-09T03:00:00Z"), ZoneOffset.UTC);
        PayrollItemEnrollmentService service = new PayrollItemEnrollmentService(
                policyRepository, enrollmentRepository, clock,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mock(PayrollItemParameterDefinitionRepository.class),
                new PayrollItemRuleParameterResolverRegistry(List.of())
        );

        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(5L);
        policy.setApplicationScope(PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT);
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
        assertThat(captor.getValue().getEffectiveTo()).isNull();
        assertThat(captor.getValue().getDeletedAt())
                .isEqualTo(Instant.parse("2026-08-09T03:00:00Z"));
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
                mock(PayrollItemParameterDefinitionRepository.class),
                new PayrollItemRuleParameterResolverRegistry(List.of())
        );

        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(5L);
        policy.setApplicationScope(PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT);
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
    }

    @Test
    void synchronize_shouldCloseEnrollmentUsingExplicitOperationDate() {
        TenantContext.setTenantId("default");
        var policyRepository = mock(PayrollItemBalancePolicyRepository.class);
        var enrollmentRepository = mock(EmployeePayrollItemEnrollmentRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-15T03:00:00Z"), ZoneOffset.UTC);
        PayrollItemEnrollmentService service = new PayrollItemEnrollmentService(
                policyRepository, enrollmentRepository, clock,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                mock(PayrollItemParameterDefinitionRepository.class),
                new PayrollItemRuleParameterResolverRegistry(List.of())
        );

        PayrollItemBalancePolicy policy = employeeEnrollmentPolicy(5L);
        EmployeePayrollItemEnrollment current = enrollment(
                LocalDate.of(2026, 4, 1), "{}"
        );
        stubCurrent(
                policyRepository,
                enrollmentRepository,
                policy,
                Optional.of(current)
        );

        service.synchronize(
                10L,
                PayrollItemTargetType.DEDUCTION,
                "DORMITORY_FEE",
                false,
                Map.of(),
                LocalDate.of(2026, 6, 1)
        );

        assertThat(current.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(current.getDeletedAt()).isNull();
    }

    @Test
    void synchronize_shouldVersionParameterChangesAtExplicitOperationDate() {
        TenantContext.setTenantId("default");
        var policyRepository = mock(PayrollItemBalancePolicyRepository.class);
        var enrollmentRepository = mock(EmployeePayrollItemEnrollmentRepository.class);
        var parameterRepository = mock(PayrollItemParameterDefinitionRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-15T03:00:00Z"), ZoneOffset.UTC);
        PayrollItemEnrollmentService service = new PayrollItemEnrollmentService(
                policyRepository, enrollmentRepository, clock,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                parameterRepository,
                new PayrollItemRuleParameterResolverRegistry(List.of())
        );

        PayrollItemBalancePolicy policy = employeeEnrollmentPolicy(5L);
        EmployeePayrollItemEnrollment current = enrollment(
                LocalDate.of(2026, 4, 1), "{\"amount\":\"100\"}"
        );
        stubCurrent(
                policyRepository,
                enrollmentRepository,
                policy,
                Optional.of(current)
        );
        PayrollItemParameterDefinition amount = new PayrollItemParameterDefinition();
        amount.setBalancePolicyId(5L);
        amount.setParameterKey("amount");
        amount.setDisplayName("金額");
        amount.setInputType(PayrollItemParameterInputType.NUMBER);
        amount.setActiveFlag(true);
        when(parameterRepository
                .findAllByTenantIdAndBalancePolicyIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        "default", 5L
                )).thenReturn(List.of(amount));

        service.synchronize(
                10L,
                PayrollItemTargetType.DEDUCTION,
                "DORMITORY_FEE",
                true,
                Map.of("amount", "200"),
                LocalDate.of(2026, 6, 1)
        );

        assertThat(current.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 5, 31));
        var captor = org.mockito.ArgumentCaptor
                .forClass(EmployeePayrollItemEnrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getEffectiveFrom())
                .isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(captor.getValue().getEffectiveTo()).isNull();
        assertThat(captor.getValue().getSettingsJson()).isEqualTo("{\"amount\":\"200\"}");
    }

    @Test
    void synchronize_shouldMaterializeResolvedParameterIntoEnrollmentHistory() {
        TenantContext.setTenantId("default");
        var policyRepository = mock(PayrollItemBalancePolicyRepository.class);
        var enrollmentRepository = mock(EmployeePayrollItemEnrollmentRepository.class);
        var parameterRepository = mock(PayrollItemParameterDefinitionRepository.class);
        var resolverRegistry = mock(PayrollItemRuleParameterResolverRegistry.class);
        PayrollItemEnrollmentService service = new PayrollItemEnrollmentService(
                policyRepository, enrollmentRepository, Clock.systemUTC(),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                parameterRepository, resolverRegistry
        );

        PayrollItemBalancePolicy policy = employeeEnrollmentPolicy(5L);
        stubCurrent(
                policyRepository, enrollmentRepository, policy,
                Optional.empty()
        );
        PayrollItemParameterDefinition dailyAmount =
                new PayrollItemParameterDefinition();
        dailyAmount.setBalancePolicyId(5L);
        dailyAmount.setParameterKey("dailyAmount");
        dailyAmount.setDisplayName("日額");
        dailyAmount.setInputType(PayrollItemParameterInputType.NUMBER);
        dailyAmount.setRequiredFlag(true);
        dailyAmount.setDefaultValue("0");
        dailyAmount.setRuleValueResolverKey(
                "SELECT_OPTION_CALCULATION_VALUE:type");
        dailyAmount.setActiveFlag(true);
        when(parameterRepository
                .findAllByTenantIdAndBalancePolicyIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        "default", 5L
                )).thenReturn(List.of(dailyAmount));
        when(resolverRegistry.resolve(any(), any()))
                .thenReturn(new java.math.BigDecimal("1500"));

        service.synchronize(
                10L, PayrollItemTargetType.DEDUCTION, "DORMITORY_FEE",
                true, Map.of(), LocalDate.of(2026, 9, 1)
        );

        var captor = org.mockito.ArgumentCaptor
                .forClass(EmployeePayrollItemEnrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertThat(captor.getValue().getSettingsJson())
                .isEqualTo("{\"dailyAmount\":\"1500\"}");
    }

    private static PayrollItemBalancePolicy employeeEnrollmentPolicy(Long id) {
        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(id);
        policy.setApplicationScope(PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT);
        return policy;
    }

    private static EmployeePayrollItemEnrollment enrollment(
            LocalDate effectiveFrom,
            String settingsJson
    ) {
        EmployeePayrollItemEnrollment enrollment = new EmployeePayrollItemEnrollment();
        enrollment.setEmployeeId(10L);
        enrollment.setBalancePolicyId(5L);
        enrollment.setEffectiveFrom(effectiveFrom);
        enrollment.setSettingsJson(settingsJson);
        return enrollment;
    }

    private static void stubCurrent(
            PayrollItemBalancePolicyRepository policyRepository,
            EmployeePayrollItemEnrollmentRepository enrollmentRepository,
            PayrollItemBalancePolicy policy,
            Optional<EmployeePayrollItemEnrollment> current
    ) {
        when(policyRepository.findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                "default", PayrollItemTargetType.DEDUCTION, "DORMITORY_FEE"
        )).thenReturn(Optional.of(policy));
        when(enrollmentRepository
                .findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                        10L, 5L
                )).thenReturn(current);
    }
}
