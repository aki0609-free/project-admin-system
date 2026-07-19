package com.project.backend.features.system.rule.dto;

import java.util.Map;

import lombok.Builder;

@Builder
public record RuleExecutionResult(
        String ruleName,
        boolean executed,
        Object result,
        Map<String, Object> facts,
        String message
) {
}