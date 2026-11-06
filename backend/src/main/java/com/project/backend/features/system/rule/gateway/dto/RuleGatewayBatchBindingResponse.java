package com.project.backend.features.system.rule.gateway.dto;

import com.project.backend.features.system.rule.gateway.RuleGatewayBinding;
import com.project.backend.features.system.rule.gateway.RuleGatewayContract;
import com.project.backend.features.system.rule.gateway.RuleGatewayResultShape;

public record RuleGatewayBatchBindingResponse(
        String outputKey,
        String gatewayKey,
        String ruleName,
        Integer ruleVersion,
        RuleGatewayResultShape resultShape
) {
    public static RuleGatewayBatchBindingResponse from(
            String outputKey,
            RuleGatewayBinding binding,
            RuleGatewayContract contract
    ) {
        return new RuleGatewayBatchBindingResponse(
                outputKey,
                binding.gatewayKey(),
                binding.ruleName(),
                binding.ruleVersion(),
                contract.resultShape()
        );
    }
}
