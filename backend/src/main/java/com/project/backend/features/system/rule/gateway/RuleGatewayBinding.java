package com.project.backend.features.system.rule.gateway;

import java.util.Map;

public record RuleGatewayBinding(
        String gatewayKey,
        String ruleName,
        Integer ruleVersion,
        Map<String, Object> parameters
) {
    public RuleGatewayBinding {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    public RuleGatewayBinding(
            String gatewayKey,
            String ruleName,
            Integer ruleVersion
    ) {
        this(gatewayKey, ruleName, ruleVersion, Map.of());
    }
}
