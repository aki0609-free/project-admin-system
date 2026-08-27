package com.project.backend.features.system.excelbook.mapper;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.excelbook.dto.ExcelBookMasterRequest;
import com.project.backend.features.system.excelbook.dto.ExcelBookMasterResponse;
import com.project.backend.features.system.excelbook.dto.ExcelBookPrintConfig;
import com.project.backend.features.system.excelbook.dto.ExcelBookSelectionConfig;
import com.project.backend.features.system.excelbook.dto.ExcelBookVariableMappingRequest;
import com.project.backend.features.system.excelbook.dto.ExcelBookVariableMappingResponse;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.entity.ExcelBookVariableMapping;
import com.project.backend.features.system.excelbook.enums.ExcelBookLayoutType;
import com.project.backend.features.system.excelbook.enums.ExcelBookSourceType;

@Component
public class ExcelBookMasterMapper {

    public ExcelBookMasterResponse toResponse(ExcelBookMaster entity) {
        return toResponse(entity, true);
    }

    public ExcelBookMasterResponse toResponse(
            ExcelBookMaster entity,
            boolean templateRequired
    ) {
        return new ExcelBookMasterResponse(
                entity.getId(),
                entity.getBookCode(),
                entity.getBookName(),
                entity.getSourceType(),
                entity.getLayoutType(),
                entity.getRendererKey() == null
                        ? entity.getLayoutType().name()
                        : entity.getRendererKey(),
                new ExcelBookSelectionConfig(
                        entity.getSelectionMode(),
                        entity.getSelectionSourceName(),
                        entity.getSelectionValueColumn(),
                        splitColumns(entity.getSelectionDisplayColumns()),
                        entity.getAllowSelectAll(),
                        entity.getGenerationUnit()
                ),
                new ExcelBookPrintConfig(
                        entity.getPrintPaperSize(),
                        entity.getPrintOrientation(),
                        entity.getPrintFitToOnePage()
                ),
                entity.getSourceName(),
                entity.getTemplateSheetName(),
                templateRequired,
                entity.getActiveFlag(),
                entity.getVariableMappings().stream()
                        .map(mapping ->
                                new ExcelBookVariableMappingResponse(
                                        mapping.getId(),
                                        mapping.getVariableKey(),
                                        mapping.getSourceColumn(),
                                        mapping.getScope(),
                                        mapping.getDataType(),
                                        mapping.getOrderNo()
                                )
                        )
                        .toList()
        );
    }

    public ExcelBookMaster toEntity(ExcelBookMasterRequest request) {
        ExcelBookMaster entity = new ExcelBookMaster();
        entity.setTemplateFilePath("");
        entity.setOutputFilePath("");
        apply(entity, request);
        return entity;
    }

    public void apply(ExcelBookMaster entity, ExcelBookMasterRequest request) {
        entity.setBookCode(request.bookCode());
        entity.setBookName(request.bookName());
        entity.setSourceType(request.sourceType() == null ? ExcelBookSourceType.SNAPSHOT : request.sourceType());
        entity.setLayoutType(
                request.layoutType() == null
                        ? ExcelBookLayoutType.REPEATING_ROW
                        : request.layoutType()
        );
        entity.setRendererKey(
                request.rendererKey() == null
                        || request.rendererKey().isBlank()
                        ? entity.getLayoutType().name()
                        : request.rendererKey()
        );
        ExcelBookSelectionConfig selection = request.selection();
        entity.setSelectionMode(selection.mode());
        entity.setSelectionSourceName(selection.dataSourceCode());
        entity.setSelectionValueColumn(selection.valueColumn());
        entity.setSelectionDisplayColumns(
                String.join(",", selection.displayColumns())
        );
        entity.setAllowSelectAll(selection.allowSelectAll());
        entity.setGenerationUnit(selection.generationUnit());
        ExcelBookPrintConfig print = request.print();
        entity.setPrintPaperSize(print.paperSize());
        entity.setPrintOrientation(print.orientation());
        entity.setPrintFitToOnePage(print.fitToOnePage());
        entity.setSourceName(request.dataSourceCode());
        entity.setTemplateSheetName(
                request.templateSheetName() == null || request.templateSheetName().isBlank()
                        ? "TEMPLATE"
                        : request.templateSheetName()
        );
        entity.setActiveFlag(request.activeFlag() == null ? true : request.activeFlag());
        entity.clearVariableMappings();
        request.variableMappings().forEach(mapping ->
                entity.addVariableMapping(toVariableMapping(mapping))
        );
    }

    private ExcelBookVariableMapping toVariableMapping(
            ExcelBookVariableMappingRequest request
    ) {
        ExcelBookVariableMapping mapping =
                new ExcelBookVariableMapping();
        mapping.setVariableKey(request.variableKey());
        mapping.setSourceColumn(request.sourceColumn());
        mapping.setScope(request.scope());
        mapping.setDataType(request.dataType());
        mapping.setOrderNo(
                request.orderNo() == null ? 1 : request.orderNo()
        );
        return mapping;
    }

    private java.util.List<String> splitColumns(String value) {
        if (value == null || value.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(column -> !column.isEmpty())
                .toList();
    }
}
