package com.project.backend.features.system.rule.gateway;

import com.project.backend.features.system.rule.entity.RuleMaster;

public record RuleGatewayPreparedBinding(
        RuleGatewayBinding binding,
        RuleMaster rule
) {
}
