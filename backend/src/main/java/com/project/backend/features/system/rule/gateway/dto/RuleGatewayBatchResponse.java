package com.project.backend.features.system.rule.gateway.dto;

import java.util.List;

public record RuleGatewayBatchResponse(
        List<RuleGatewayBatchBindingResponse> bindings,
        List<RuleGatewayBatchRowResponse> rows
) {
}
