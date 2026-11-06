package com.project.backend.features.master.payrollitem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.features.master.payrollitem.dto.PayrollItemMasterSnapshot;
import com.project.backend.features.master.payrollitem.dto.PayrollItemValueRequest;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.master.payrollitem.provider.PayrollItemValueProvider;
import com.project.backend.features.system.rule.gateway.RuleGatewayExecution;
import com.project.backend.features.system.rule.gateway.RuleGatewayResultShape;
import com.project.backend.features.system.rule.gateway.RuleGatewayService;

class PayrollItemValueServiceGatewayTest {

    @Test
    void autoItemExecutesThroughGatewayWithoutPerRuleAdapter() {
        PayrollItemValueProvider provider = mock(
                PayrollItemValueProvider.class
        );
        RuleGatewayService gatewayService = mock(RuleGatewayService.class);
        when(provider.supports()).thenReturn(PayrollItemTargetType.ALLOWANCE);
        PayrollItemMasterSnapshot master = master();
        PayrollItemValueRequest request = new PayrollItemValueRequest(
                PayrollItemTargetType.ALLOWANCE,
                42L,
                null,
                null,
                Map.of("mileage", 50)
        );
        when(provider.findMaster(request)).thenReturn(master);
        Map<String, Object> parameters = Map.of(
                "mileage", 50,
                "targetType", "ALLOWANCE",
                "targetMasterId", 42L,
                "targetCode", "DRIVER_ALLOWANCE"
        );
        when(gatewayService.executeBound(
                eq(PayrollItemRuleGatewayBindingResolver.binding(master)),
                eq(parameters)
        )).thenReturn(new RuleGatewayExecution(
                "PAYROLL_ITEM.ALLOWANCE.42",
                "DRIVER_ALLOWANCE_RULE",
                null,
                RuleGatewayResultShape.SCALAR,
                new BigDecimal("950"),
                Map.of("result", new BigDecimal("950"))
        ));
        PayrollItemValueService service = new PayrollItemValueService(
                List.of(provider),
                gatewayService,
                new PayrollMoneyPolicy()
        );

        var result = service.calculate(request);

        assertThat(result.amount()).isEqualByComparingTo("950");
        assertThat(result.ruleName()).isEqualTo("DRIVER_ALLOWANCE_RULE");
        assertThat(result.facts()).containsEntry(
                "result",
                new BigDecimal("950")
        );
        verify(gatewayService).executeBound(
                PayrollItemRuleGatewayBindingResolver.binding(master),
                parameters
        );
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
