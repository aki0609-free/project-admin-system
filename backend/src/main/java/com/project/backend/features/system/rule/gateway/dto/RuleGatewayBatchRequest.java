package com.project.backend.features.system.rule.gateway.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record RuleGatewayBatchRequest(
        LocalDate targetDate,
        RuleGatewayFailureMode failureMode,
        @NotEmpty @Size(max = 500)
        List<@Valid RuleGatewayBatchRowRequest> rows,
        @NotEmpty @Size(max = 50)
        List<@Valid RuleGatewayBatchCalculationRequest> calculations
) {
}
