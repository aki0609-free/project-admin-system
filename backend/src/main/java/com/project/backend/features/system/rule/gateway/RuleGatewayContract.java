package com.project.backend.features.system.rule.gateway;

import java.util.Set;

public record RuleGatewayContract(
        String gatewayKey,
        RuleGatewayResultShape resultShape,
        Set<String> requiredParameters,
        boolean frontendExecutable
) {
    public RuleGatewayContract {
        requiredParameters = requiredParameters == null
                ? Set.of()
                : Set.copyOf(requiredParameters);
    }
}
