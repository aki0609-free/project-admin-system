package com.project.backend.features.system.rule.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record RuleDataSourceResponse(
        Long id,
        String sourceName,
        String tableName,
        String whereClause,
        boolean singleRowFlag,
        boolean activeFlag,
        int orderNo,
        List<RuleColumnMappingResponse> columns
) {
}