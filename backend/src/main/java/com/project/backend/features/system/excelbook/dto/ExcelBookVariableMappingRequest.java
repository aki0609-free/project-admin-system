package com.project.backend.features.system.excelbook.dto;

public record ExcelBookVariableMappingRequest(
        String variableKey,
        String sourceColumn,
        String scope,
        String dataType,
        Integer orderNo
) {
}
