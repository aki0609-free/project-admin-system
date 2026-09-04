package com.project.backend.features.master.payrollitem.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.master.payrollitem.enums.PayrollItemInputSource;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

class PayrollItemPolicyServiceTest {

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void synchronize_shouldCreateEmployeePolicyAndParameterDefinitions() {
        TenantContext.setTenantId("tenant-a");
        var policyRepository = mock(PayrollItemBalancePolicyRepository.class);
        var definitionRepository = mock(PayrollItemParameterDefinitionRepository.class);
        when(policyRepository
                .findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                        "tenant-a", PayrollItemTargetType.DEDUCTION, "CUSTOM_FEE"))
                .thenReturn(Optional.empty());
        when(policyRepository.save(any(PayrollItemBalancePolicy.class)))
                .thenAnswer(invocation -> {
                    PayrollItemBalancePolicy policy = invocation.getArgument(0);
                    policy.setId(10L);
                    return policy;
                });
        when(definitionRepository
                .findAllByTenantIdAndBalancePolicyIdOrderByDisplayOrderAscIdAsc(
                        "tenant-a", 10L))
                .thenReturn(List.of());

        PayrollItemPolicyService service = new PayrollItemPolicyService(
                policyRepository, definitionRepository, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-22T00:00:00Z"), ZoneOffset.UTC));
        service.synchronize(
                PayrollItemTargetType.DEDUCTION, 20L, "CUSTOM_FEE", "独自控除",
                new PayrollItemPolicySaveRequest(
                        PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT,
                        PayrollItemInputSource.DAILY_REPORT,
                        true, BalanceUnit.DAYS, "MONTHLY",
                        "CALENDAR_DAYS_IN_ENROLLMENT", true, false,
                        List.of(new PayrollItemParameterDefinitionSaveRequest(
                                "chargeDays", "支払日数",
                                PayrollItemParameterInputType.NUMBER,
                                true, "1", List.of(), true,
                                true, false, 10))));

        ArgumentCaptor<PayrollItemBalancePolicy> policyCaptor =
                ArgumentCaptor.forClass(PayrollItemBalancePolicy.class);
        verify(policyRepository).save(policyCaptor.capture());
        assertThat(policyCaptor.getValue().getApplicationScope())
                .isEqualTo(PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT);

        ArgumentCaptor<PayrollItemParameterDefinition> definitionCaptor =
                ArgumentCaptor.forClass(PayrollItemParameterDefinition.class);
        verify(definitionRepository).save(definitionCaptor.capture());
        assertThat(definitionCaptor.getValue().getParameterKey())
                .isEqualTo("chargeDays");
        assertThat(definitionCaptor.getValue().isRuleParameterFlag()).isTrue();
    }

    @Test
    void synchronize_shouldRejectTransactionInputForAllEmployees() {
        TenantContext.setTenantId("tenant-a");
        PayrollItemPolicyService service = new PayrollItemPolicyService(
                mock(PayrollItemBalancePolicyRepository.class),
                mock(PayrollItemParameterDefinitionRepository.class),
                new ObjectMapper(), Clock.systemUTC());

        assertThatThrownBy(() -> service.synchronize(
                PayrollItemTargetType.ALLOWANCE, 1L, "BONUS", "臨時手当",
                new PayrollItemPolicySaveRequest(
                        PayrollItemApplicationScope.ALL_EMPLOYEES,
                        PayrollItemInputSource.TRANSACTION,
                        false, BalanceUnit.AMOUNT, null, null,
                        false, false, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("従業員別適用");
    }

    @Test
    void synchronize_shouldRejectAmountUnitForCalendarDayAccrual() {
        TenantContext.setTenantId("tenant-a");
        PayrollItemPolicyService service = new PayrollItemPolicyService(
                mock(PayrollItemBalancePolicyRepository.class),
                mock(PayrollItemParameterDefinitionRepository.class),
                new ObjectMapper(), Clock.systemUTC());

        assertThatThrownBy(() -> service.synchronize(
                PayrollItemTargetType.DEDUCTION, 1L,
                "DORMITORY_FEE", "寮費",
                new PayrollItemPolicySaveRequest(
                        PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT,
                        PayrollItemInputSource.DAILY_REPORT,
                        true, BalanceUnit.AMOUNT, "MONTHLY",
                        "CALENDAR_DAYS_IN_ENROLLMENT",
                        true, false, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("残高単位を日数");
    }

    @Test
    void synchronize_shouldRequireAmountUnitForCalendarDaysTimesParameter() {
        TenantContext.setTenantId("tenant-a");
        PayrollItemPolicyService service = new PayrollItemPolicyService(
                mock(PayrollItemBalancePolicyRepository.class),
                mock(PayrollItemParameterDefinitionRepository.class),
                new ObjectMapper(), Clock.systemUTC());

        assertThatThrownBy(() -> service.synchronize(
                PayrollItemTargetType.DEDUCTION, 1L,
                "GENERIC_DAILY_FEE", "日額控除",
                new PayrollItemPolicySaveRequest(
                        PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT,
                        PayrollItemInputSource.DAILY_REPORT,
                        true, BalanceUnit.DAYS, "MONTHLY",
                        "CALENDAR_DAYS_TIMES_PARAMETER:dailyAmount",
                        true, false, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("残高単位を金額");
    }

    @Test
    void synchronize_shouldRejectDuplicateSelectOptionValues() {
        TenantContext.setTenantId("tenant-a");
        PayrollItemPolicyService service = new PayrollItemPolicyService(
                mock(PayrollItemBalancePolicyRepository.class),
                mock(PayrollItemParameterDefinitionRepository.class),
                new ObjectMapper(), Clock.systemUTC());

        assertThatThrownBy(() -> service.synchronize(
                PayrollItemTargetType.DEDUCTION, 1L, "CUSTOM_FEE", "独自控除",
                employeePolicy(List.of(new PayrollItemParameterDefinitionSaveRequest(
                        "type", "種別", PayrollItemParameterInputType.SELECT,
                        true, "A", List.of(
                                new PayrollItemParameterOption("A", "A"),
                                new PayrollItemParameterOption("B", "A")
                        ), false, false, false, 10)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重複");
    }

    @Test
    void synchronize_shouldRejectResolverWithoutSelectSource() {
        TenantContext.setTenantId("tenant-a");
        PayrollItemPolicyService service = new PayrollItemPolicyService(
                mock(PayrollItemBalancePolicyRepository.class),
                mock(PayrollItemParameterDefinitionRepository.class),
                new ObjectMapper(), Clock.systemUTC());

        assertThatThrownBy(() -> service.synchronize(
                PayrollItemTargetType.DEDUCTION, 1L, "CUSTOM_FEE", "独自控除",
                employeePolicy(List.of(new PayrollItemParameterDefinitionSaveRequest(
                        "dailyAmount", "日額", PayrollItemParameterInputType.NUMBER,
                        true, "0", List.of(), true, false, false,
                        "SELECT_OPTION_CALCULATION_VALUE:missingType", 10)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SELECT項目");
    }

    @Test
    void synchronize_shouldAcceptSelectCalculationValueResolver() {
        TenantContext.setTenantId("tenant-a");
        var policyRepository = mock(PayrollItemBalancePolicyRepository.class);
        var definitionRepository = mock(PayrollItemParameterDefinitionRepository.class);
        when(policyRepository
                .findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                        "tenant-a", PayrollItemTargetType.DEDUCTION, "CUSTOM_FEE"))
                .thenReturn(Optional.empty());
        when(policyRepository.save(any(PayrollItemBalancePolicy.class)))
                .thenAnswer(invocation -> {
                    PayrollItemBalancePolicy policy = invocation.getArgument(0);
                    policy.setId(10L);
                    return policy;
                });
        when(definitionRepository
                .findAllByTenantIdAndBalancePolicyIdOrderByDisplayOrderAscIdAsc(
                        "tenant-a", 10L))
                .thenReturn(List.of());
        PayrollItemPolicyService service = new PayrollItemPolicyService(
                policyRepository, definitionRepository,
                new ObjectMapper(), Clock.systemUTC());

        service.synchronize(
                PayrollItemTargetType.DEDUCTION, 1L, "CUSTOM_FEE", "独自控除",
                employeePolicy(List.of(
                        new PayrollItemParameterDefinitionSaveRequest(
                                "type", "種別", PayrollItemParameterInputType.SELECT,
                                true, "A", List.of(
                                        new PayrollItemParameterOption(
                                                "A", "A", BigDecimal.valueOf(100))
                                ), false, false, false, 10),
                        new PayrollItemParameterDefinitionSaveRequest(
                                "dailyAmount", "日額", PayrollItemParameterInputType.NUMBER,
                                true, "0", List.of(), true, false, false,
                                "SELECT_OPTION_CALCULATION_VALUE:type", 20)
                )));

        verify(definitionRepository,
                org.mockito.Mockito.times(2)).save(any(PayrollItemParameterDefinition.class));
    }

    private PayrollItemPolicySaveRequest employeePolicy(
            List<PayrollItemParameterDefinitionSaveRequest> definitions
    ) {
        return new PayrollItemPolicySaveRequest(
                PayrollItemApplicationScope.EMPLOYEE_ENROLLMENT,
                PayrollItemInputSource.DAILY_REPORT,
                false, BalanceUnit.AMOUNT, null, null,
                false, false, definitions
        );
    }
}
