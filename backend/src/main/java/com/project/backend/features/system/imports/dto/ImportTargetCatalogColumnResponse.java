package com.project.backend.features.system.imports.dto;

import com.project.backend.features.system.imports.enums.ImportDataType;

public record ImportTargetCatalogColumnResponse(
        String columnName,
        String displayName,
        ImportDataType dataType,
        int orderNo
) {
}
