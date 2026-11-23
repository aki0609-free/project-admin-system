package com.project.backend.features.master.payrollitem.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.master.allowance.repository.AllowanceMasterRepository;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemInputSource;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.parameter.PayrollItemRuleParameterResolverRegistry;

class EmployeePayrollItemDailyDefaultTest {

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void findDailyRuleParameters_shouldReturnEmployeeAmountOnlyInsideEffectivePeriod() {
        TenantContext.setTenantId("default");
        Fixture fixture = fixture();
        PayrollItemBalancePolicy policy = policy(11L, PayrollItemTargetType.ALLOWANCE);
        EmployeePayrollItemEnrollment enrollment = enrollment(
                LocalDate.of(2026, 10, 31),
                null,
                "{\"employeeDailyAmount\":\"1250\"}"
        );
        PayrollItemParameterDefinition amountDefinition = amountDefinition(11L);

        when(fixture.policyRepository
                .findAllByTenantIdAndDeletedAtIsNullOrderByIdAsc("default"))
                .thenReturn(List.of(policy));
        when(fixture.enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        20L, 11L, LocalDate.of(2026, 11, 1)))
                .thenReturn(List.of(enrollment));
        when(fixture.parameterRepository
                .findAllByTenantIdAndBalancePolicyIdAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        "default", 11L))
                .thenReturn(List.of(amountDefinition));

        assertThat(fixture.service.findDailyRuleParameters(
                20L,
                PayrollItemTargetType.ALLOWANCE,
                LocalDate.of(2026, 10, 30)
        )).isEmpty();
        assertThat(fixture.service.findDailyRuleParameters(
                20L,
                PayrollItemTargetType.ALLOWANCE,
                LocalDate.of(2026, 11, 1)
        ).get(101L).get("employeeDailyAmount").toString()).isEqualTo("1250");
    }

    @Test
    void findExcludedDailyMasterIds_shouldExcludeEmployeeItemWithoutEnrollment() {
        TenantContext.setTenantId("default");
        Fixture fixture = fixture();
        PayrollItemBalancePolicy policy = policy(12L, PayrollItemTargetType.DEDUCTION);
        when(fixture.policyRepository
                .findAllByTenantIdAndDeletedAtIsNullOrderByIdAsc("default"))
                .thenReturn(List.of(policy));
        when(fixture.enrollmentRepository
                .findAllByEmployeeIdAndBalancePolicyIdAndEffectiveFromLessThanEqualAndDeletedAtIsNullOrderByEffectiveFromAsc(
                        20L, 12L, LocalDate.of(2026, 11, 1)))
                .thenReturn(List.of());

        assertThat(fixture.service.findExcludedDailyMasterIds(
                20L,
                PayrollItemTargetType.DEDUCTION,
                LocalDate.of(2026, 11, 1)
        )).containsExactly(101L);
    }

    private Fixture fixture() {
        PayrollItemBalancePolicyRepository policyRepository =
                mock(PayrollItemBalancePolicyRepository.class);
        EmployeePayrollItemEnrollmentRepository enrollmentRepository =
                mock(EmployeePayrollItemEnrollmentRepository.class);
        PayrollItemParameterDefinitionRepository parameterRepository =
                mock(PayrollItemParameterDefinitionRepository.class);
        EmployeePayrollItemSettingService service =
                new EmployeePayrollItemSettingService(
                        policyRepository,
                        enrollmentRepository,
                        mock(PayrollItemBalanceQueryService.class),
                        mock(PayrollItemEnrollmentService.class),
                        new ObjectMapper(),
                        Clock.systemUTC(),
                        mock(DeductionMasterRepository.class),
                        mock(AllowanceMasterRepository.class),
                        parameterRepository,
                        new PayrollItemRuleParameterResolverRegistry(List.of())
                );
        return new Fixture(
                policyRepository,
                enrollmentRepository,
                parameterRepository,
                service
        );
    }

    private PayrollItemBalancePolicy policy(
            Long id,
            PayrollItemTargetType targetType
    ) {
        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(id);
        policy.setTargetType(targetType);
        policy.setTargetMasterId(101L);
        policy.setTargetCode(targetType == PayrollItemTargetType.ALLOWANCE
                ? "ATTENDANCE_ATTITUDE" : "LEGAL_DEPOSIT");
        policy.setApplicationScope(PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT);
        policy.setInputSource(PayrollItemInputSource.DAILY_REPORT);
        policy.setActiveFlag(true);
        return policy;
    }

    private EmployeePayrollItemEnrollment enrollment(
            LocalDate from,
            LocalDate to,
            String settingsJson
    ) {
        EmployeePayrollItemEnrollment enrollment =
                new EmployeePayrollItemEnrollment();
        enrollment.setEffectiveFrom(from);
        enrollment.setEffectiveTo(to);
        enrollment.setSettingsJson(settingsJson);
        return enrollment;
    }

    private PayrollItemParameterDefinition amountDefinition(Long policyId) {
        PayrollItemParameterDefinition definition =
                new PayrollItemParameterDefinition();
        definition.setBalancePolicyId(policyId);
        definition.setParameterKey("employeeDailyAmount");
        definition.setDisplayName("日報初期額");
        definition.setInputType(PayrollItemParameterInputType.NUMBER);
        definition.setDefaultValue("0");
        definition.setRuleParameterFlag(true);
        definition.setActiveFlag(true);
        return definition;
    }

    private record Fixture(
            PayrollItemBalancePolicyRepository policyRepository,
            EmployeePayrollItemEnrollmentRepository enrollmentRepository,
            PayrollItemParameterDefinitionRepository parameterRepository,
            EmployeePayrollItemSettingService service
    ) {
    }
}
