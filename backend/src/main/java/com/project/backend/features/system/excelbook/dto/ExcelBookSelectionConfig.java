package com.project.backend.features.system.excelbook.dto;

import java.util.List;

import com.project.backend.features.system.excelbook.enums.ExcelBookGenerationUnit;
import com.project.backend.features.system.excelbook.enums.ExcelBookSelectionMode;

public record ExcelBookSelectionConfig(
        ExcelBookSelectionMode mode,
        String dataSourceCode,
        String valueColumn,
        List<String> displayColumns,
        Boolean allowSelectAll,
        ExcelBookGenerationUnit generationUnit
) {
    public ExcelBookSelectionConfig {
        mode = mode == null ? ExcelBookSelectionMode.NONE : mode;
        displayColumns = displayColumns == null
                ? List.of()
                : List.copyOf(displayColumns);
        allowSelectAll = allowSelectAll != null && allowSelectAll;
        generationUnit = generationUnit == null
                ? ExcelBookGenerationUnit.ONE_FILE
                : generationUnit;
    }

    public static ExcelBookSelectionConfig none() {
        return new ExcelBookSelectionConfig(
                ExcelBookSelectionMode.NONE,
                null,
                null,
                List.of(),
                false,
                ExcelBookGenerationUnit.ONE_FILE
        );
    }
}
