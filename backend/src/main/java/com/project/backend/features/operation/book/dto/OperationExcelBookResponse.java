package com.project.backend.features.operation.book.dto;

import java.util.List;

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
        boolean monthlyClosingConfigured,
        List<String> readinessIssues,
        ExcelBookSelectionConfig selection,
        ExcelBookPrintConfig print
) {
}
