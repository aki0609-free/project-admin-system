package com.project.backend.features.master.payrollitem.service;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.rule.gateway.RuleGatewayContract;
import com.project.backend.features.system.rule.gateway.RuleGatewayDynamicContractProvider;
import com.project.backend.features.system.rule.gateway.RuleGatewayResultShape;

@Component
public class PayrollItemRuleGatewayContractProvider
        implements RuleGatewayDynamicContractProvider {

    @Override
    public boolean supports(String gatewayKey) {
        return PayrollItemRuleGatewayKeys.supports(gatewayKey);
    }

    @Override
    public RuleGatewayContract contract(String gatewayKey) {
        PayrollItemRuleGatewayKeys.parse(gatewayKey);
        return new RuleGatewayContract(
                gatewayKey,
                RuleGatewayResultShape.SCALAR,
                Set.of("targetType", "targetMasterId", "targetCode"),
                true
        );
    }
}
