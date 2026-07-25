package com.project.backend.features.system.imports.dto;

import java.util.List;

public record ImportTargetCatalogResponse(
        String tableName,
        String displayName,
        String description,
        boolean tenantScopedFlag,
        boolean allowDeleteInsertFlag,
        List<ImportTargetCatalogColumnResponse> columns
) {
}
