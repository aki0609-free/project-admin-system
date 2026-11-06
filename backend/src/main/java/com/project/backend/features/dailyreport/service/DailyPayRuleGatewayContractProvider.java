package com.project.backend.features.dailyreport.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.project.backend.features.dailyreport.enums.DailyPayComponentType;
import com.project.backend.features.system.rule.gateway.RuleGatewayContract;
import com.project.backend.features.system.rule.gateway.RuleGatewayContractProvider;
import com.project.backend.features.system.rule.gateway.RuleGatewayResultShape;

@Component
public class DailyPayRuleGatewayContractProvider
        implements RuleGatewayContractProvider {

    @Override
    public List<RuleGatewayContract> contracts() {
        return Arrays.stream(DailyPayComponentType.values())
                .map(componentType -> new RuleGatewayContract(
                        DailyPayRuleGatewayKeys.forComponent(componentType),
                        RuleGatewayResultShape.SCALAR,
                        Set.of("componentType", "targetDate"),
                        true
                ))
                .toList();
    }
}
