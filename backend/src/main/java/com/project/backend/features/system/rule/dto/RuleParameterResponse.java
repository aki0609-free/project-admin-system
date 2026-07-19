package com.project.backend.features.system.rule.dto;

import com.project.backend.features.system.rule.enums.RuleDataType;
import lombok.Builder;

@Builder
public record RuleParameterResponse(
        Long id,
        String paramName,
        RuleDataType dataType,
        boolean requiredFlag,
        String defaultValue,
        String description,
        int orderNo
) {
}