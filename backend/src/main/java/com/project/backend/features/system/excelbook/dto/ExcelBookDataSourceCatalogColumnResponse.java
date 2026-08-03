package com.project.backend.features.system.excelbook.dto;

public record ExcelBookDataSourceCatalogColumnResponse(
        String columnName,
        String displayName,
        String dataType,
        Integer orderNo
) {
}
