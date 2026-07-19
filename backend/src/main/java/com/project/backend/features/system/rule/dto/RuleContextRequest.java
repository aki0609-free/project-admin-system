package com.project.backend.features.system.rule.dto;

import java.util.Map;

import lombok.Builder;

@Builder
public record RuleContextRequest(
        Map<String, Object> parameters
) {
}