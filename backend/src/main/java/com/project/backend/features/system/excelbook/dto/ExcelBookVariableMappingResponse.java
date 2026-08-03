package com.project.backend.features.system.excelbook.dto;

public record ExcelBookVariableMappingResponse(
        Long id,
        String variableKey,
        String sourceColumn,
        String scope,
        String dataType,
        Integer orderNo
) {
}
