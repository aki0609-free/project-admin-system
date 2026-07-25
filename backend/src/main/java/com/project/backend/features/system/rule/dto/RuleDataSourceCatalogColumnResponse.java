package com.project.backend.features.system.rule.dto;

import com.project.backend.features.system.rule.enums.RuleDataType;

public record RuleDataSourceCatalogColumnResponse(
        String columnName,
        String displayName,
        RuleDataType dataType,
        int orderNo
) {
}
