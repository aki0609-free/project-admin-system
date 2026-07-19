package com.project.backend.features.system.rule.dto;

import java.util.List;

public record RuleDataSourceSaveRequest(
        Long id,
        String sourceName,
        String tableName,
        String whereClause,
        boolean singleRowFlag,
        boolean activeFlag,
        int orderNo,
        List<RuleColumnMappingSaveRequest> columns
) {
}