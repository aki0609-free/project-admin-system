package com.project.backend.features.system.rule.gateway.dto;

import java.time.LocalDate;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public record RuleGatewayExecuteRequest(
        @NotBlank String gatewayKey,
        LocalDate targetDate,
        Map<String, Object> parameters
) {
}
