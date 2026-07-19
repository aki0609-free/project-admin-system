package com.project.backend.features.system.excelbook.dto;

import com.project.backend.features.system.excelbook.enums.ExcelBookSourceType;

public record ExcelBookMasterResponse(
        Long id,
        String bookCode,
        String bookName,
        String templateFilePath,
        String outputFilePath,
        ExcelBookSourceType sourceType,
        String sourceName,
        String templateSheetName,
        Boolean activeFlag
) {
}