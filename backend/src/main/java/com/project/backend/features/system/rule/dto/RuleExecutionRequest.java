package com.project.backend.features.system.rule.dto;

public record RuleExecutionRequest(
        String ruleName,
        RuleContextRequest context
) {
}