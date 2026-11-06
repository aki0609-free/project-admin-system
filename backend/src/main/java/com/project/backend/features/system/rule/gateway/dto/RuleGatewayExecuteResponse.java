package com.project.backend.features.system.rule.gateway.dto;

import com.project.backend.features.system.rule.gateway.RuleGatewayExecution;
import com.project.backend.features.system.rule.gateway.RuleGatewayResultShape;

public record RuleGatewayExecuteResponse(
        String gatewayKey,
        String ruleName,
        Integer ruleVersion,
        RuleGatewayResultShape resultShape,
        Object result
) {
    public static RuleGatewayExecuteResponse from(
            RuleGatewayExecution execution
    ) {
        return new RuleGatewayExecuteResponse(
                execution.gatewayKey(),
                execution.ruleName(),
                execution.ruleVersion(),
                execution.resultShape(),
                execution.result()
        );
    }
}
