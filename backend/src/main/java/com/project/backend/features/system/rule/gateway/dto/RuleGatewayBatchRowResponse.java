package com.project.backend.features.system.rule.gateway.dto;

import java.util.Map;

public record RuleGatewayBatchRowResponse(
        String rowKey,
        Map<String, Object> values,
        Map<String, RuleGatewayBatchErrorResponse> errors
) {
}
