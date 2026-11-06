package com.project.backend.features.system.rule.gateway;

import java.util.Map;

public record RuleGatewayExecution(
        String gatewayKey,
        String ruleName,
        Integer ruleVersion,
        RuleGatewayResultShape resultShape,
        Object result,
        Map<String, Object> facts
) {
}
