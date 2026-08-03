package com.project.backend.features.system.excelbook.dto;

import com.project.backend.features.system.excelbook.enums.ExcelBookPrintOrientation;

public record ExcelBookPrintConfig(
        String paperSize,
        ExcelBookPrintOrientation orientation,
        Boolean fitToOnePage
) {
    public ExcelBookPrintConfig {
        paperSize = paperSize == null || paperSize.isBlank()
                ? "A4"
                : paperSize;
        orientation = orientation == null
                ? ExcelBookPrintOrientation.PORTRAIT
                : orientation;
        fitToOnePage = fitToOnePage != null && fitToOnePage;
    }

    public static ExcelBookPrintConfig defaults() {
        return new ExcelBookPrintConfig(
                "A4",
                ExcelBookPrintOrientation.PORTRAIT,
                false
        );
    }
}
