package com.project.backend.features.master.payrollitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.features.master.payrollitem.dto.PayrollItemMasterSnapshot;
import com.project.backend.features.master.payrollitem.dto.PayrollItemValueRequest;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.provider.PayrollItemValueProvider;
import com.project.backend.features.system.rule.gateway.RuleGatewayBinding;

class PayrollItemRuleGatewayBindingResolverTest {

    @Test
    void resolvesDynamicAllowanceKeyToConfiguredRule() {
        PayrollItemValueProvider provider = mock(
                PayrollItemValueProvider.class
        );
        when(provider.supports()).thenReturn(PayrollItemTargetType.ALLOWANCE);
        when(provider.findMaster(new PayrollItemValueRequest(
                PayrollItemTargetType.ALLOWANCE,
                42L,
                null,
                null,
                Map.of()
        ))).thenReturn(master());
        PayrollItemRuleGatewayBindingResolver resolver =
                new PayrollItemRuleGatewayBindingResolver(List.of(provider));

        RuleGatewayBinding result = resolver.resolve(
                "PAYROLL_ITEM.ALLOWANCE.42",
                LocalDate.of(2026, 11, 6)
        );

        assertThat(result.ruleName()).isEqualTo("DRIVER_ALLOWANCE_RULE");
        assertThat(result.parameters())
                .containsEntry("targetType", "ALLOWANCE")
                .containsEntry("targetMasterId", 42L)
                .containsEntry("targetCode", "DRIVER_ALLOWANCE");
    }

    private PayrollItemMasterSnapshot master() {
        return new PayrollItemMasterSnapshot(
                PayrollItemTargetType.ALLOWANCE,
                42L,
                "DRIVER_ALLOWANCE",
                "運転手当",
                "AUTO",
                "DRIVER_ALLOWANCE_RULE",
                0,
                0,
                null,
                true,
                10
        );
    }
}
