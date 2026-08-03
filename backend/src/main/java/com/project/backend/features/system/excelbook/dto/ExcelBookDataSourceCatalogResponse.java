package com.project.backend.features.system.excelbook.dto;

import java.util.List;

public record ExcelBookDataSourceCatalogResponse(
        String sourceCode,
        String displayName,
        String description,
        List<ExcelBookDataSourceCatalogColumnResponse> columns
) {
    public ExcelBookDataSourceCatalogResponse {
        columns = List.copyOf(columns);
    }
}
