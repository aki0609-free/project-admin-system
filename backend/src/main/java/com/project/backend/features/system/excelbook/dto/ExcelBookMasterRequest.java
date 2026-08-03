package com.project.backend.features.system.excelbook.dto;

import com.project.backend.features.system.excelbook.enums.ExcelBookLayoutType;
import com.project.backend.features.system.excelbook.enums.ExcelBookSourceType;
import java.util.List;

public record ExcelBookMasterRequest(
        String bookCode,
        String bookName,
        ExcelBookSourceType sourceType,
        ExcelBookLayoutType layoutType,
        String rendererKey,
        ExcelBookSelectionConfig selection,
        ExcelBookPrintConfig print,
        String dataSourceCode,
        String templateSheetName,
        Boolean activeFlag,
        List<ExcelBookVariableMappingRequest> variableMappings
) {
    public ExcelBookMasterRequest(
            String bookCode,
            String bookName,
            ExcelBookSourceType sourceType,
            ExcelBookLayoutType layoutType,
            String dataSourceCode,
            String templateSheetName,
            Boolean activeFlag,
            List<ExcelBookVariableMappingRequest> variableMappings
    ) {
        this(
                bookCode,
                bookName,
                sourceType,
                layoutType,
                layoutType == null ? null : layoutType.name(),
                ExcelBookSelectionConfig.none(),
                ExcelBookPrintConfig.defaults(),
                dataSourceCode,
                templateSheetName,
                activeFlag,
                variableMappings
        );
    }

    public ExcelBookMasterRequest(
            String bookCode,
            String bookName,
            ExcelBookSourceType sourceType,
            String dataSourceCode,
            String templateSheetName,
            Boolean activeFlag,
            List<ExcelBookVariableMappingRequest> variableMappings
    ) {
        this(
                bookCode,
                bookName,
                sourceType,
                ExcelBookLayoutType.REPEATING_ROW,
                ExcelBookLayoutType.REPEATING_ROW.name(),
                ExcelBookSelectionConfig.none(),
                ExcelBookPrintConfig.defaults(),
                dataSourceCode,
                templateSheetName,
                activeFlag,
                variableMappings
        );
    }

    public ExcelBookMasterRequest {
        selection = selection == null
                ? ExcelBookSelectionConfig.none()
                : selection;
        print = print == null
                ? ExcelBookPrintConfig.defaults()
                : print;
        variableMappings = variableMappings == null
                ? List.of()
                : List.copyOf(variableMappings);
    }
}
