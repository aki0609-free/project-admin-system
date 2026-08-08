package com.project.backend.features.operation.book.dto;

import com.project.backend.features.system.excelbook.dto.ExcelBookPrintConfig;
import com.project.backend.features.system.excelbook.dto.ExcelBookSelectionConfig;

public record OperationExcelBookResponse(
        Long id,
        String bookCode,
        String bookName,
        String dataSourceCode,
        SpreadsheetLedgerGenerationMode generationMode,
        boolean generationReady,
        boolean templateConfigured,
        ExcelBookSelectionConfig selection,
        ExcelBookPrintConfig print
) {
}
