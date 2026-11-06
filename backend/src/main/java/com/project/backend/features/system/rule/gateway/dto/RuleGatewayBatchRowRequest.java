package com.project.backend.features.system.rule.gateway.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public record RuleGatewayBatchRowRequest(
        @NotBlank String rowKey,
        Map<String, Object> parameters
) {
}
