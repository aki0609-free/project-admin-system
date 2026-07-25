package com.project.backend.features.system.rule.dto;

import java.util.List;

public record RuleDataSourceCatalogResponse(
        String sourceCode,
        String displayName,
        String description,
        boolean tenantScopedFlag,
        int maxRows,
        List<RuleDataSourceCatalogColumnResponse> columns
) {

    public RuleDataSourceCatalogResponse {
        columns = List.copyOf(columns);
    }
}
