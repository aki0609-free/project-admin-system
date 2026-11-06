package com.project.backend.features.system.rule.gateway.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public record RuleGatewayBatchCalculationRequest(
        @NotBlank String outputKey,
        @NotBlank String gatewayKey,
        Map<String, Object> parameters
) {
}
