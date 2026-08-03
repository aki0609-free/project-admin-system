package com.project.backend.features.operation.book.dto;

import java.util.List;

import com.project.backend.features.system.excelbook.enums.ExcelBookGenerationUnit;
import com.project.backend.features.system.excelbook.enums.ExcelBookSelectionMode;

public record SpreadsheetLedgerSelectionResponse(
        ExcelBookSelectionMode mode,
        String valueColumn,
        boolean allowSelectAll,
        ExcelBookGenerationUnit generationUnit,
        List<SpreadsheetLedgerSelectionColumnResponse> columns,
        List<SpreadsheetLedgerSelectionOptionResponse> options
) {
    public SpreadsheetLedgerSelectionResponse {
        columns = List.copyOf(columns);
        options = List.copyOf(options);
    }
}
