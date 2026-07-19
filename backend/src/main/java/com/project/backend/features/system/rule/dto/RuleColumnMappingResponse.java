package com.project.backend.features.system.rule.dto;

import com.project.backend.features.system.rule.enums.RuleDataType;
import lombok.Builder;

@Builder
public record RuleColumnMappingResponse(
        Long id,
        String columnName,
        String factKey,
        RuleDataType dataType,
        boolean requiredFlag,
        int orderNo
) {
}