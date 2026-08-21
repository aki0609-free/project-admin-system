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
}
