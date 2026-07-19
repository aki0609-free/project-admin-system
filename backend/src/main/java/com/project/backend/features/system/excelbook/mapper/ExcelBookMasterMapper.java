package com.project.backend.features.system.excelbook.mapper;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.excelbook.dto.ExcelBookMasterRequest;
import com.project.backend.features.system.excelbook.dto.ExcelBookMasterResponse;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;
import com.project.backend.features.system.excelbook.enums.ExcelBookSourceType;

@Component
public class ExcelBookMasterMapper {

    public ExcelBookMasterResponse toResponse(ExcelBookMaster entity) {
        return new ExcelBookMasterResponse(
                entity.getId(),
                entity.getBookCode(),
                entity.getBookName(),
                entity.getTemplateFilePath(),
                entity.getOutputFilePath(),
                entity.getSourceType(),
                entity.getSourceName(),
                entity.getTemplateSheetName(),
                entity.getActiveFlag()
        );
    }

    public ExcelBookMaster toEntity(ExcelBookMasterRequest request) {
        ExcelBookMaster entity = new ExcelBookMaster();
        apply(entity, request);
        return entity;
    }

    public void apply(ExcelBookMaster entity, ExcelBookMasterRequest request) {
        entity.setBookCode(request.bookCode());
        entity.setBookName(request.bookName());
        entity.setTemplateFilePath(request.templateFilePath());
        entity.setOutputFilePath(request.outputFilePath());
        entity.setSourceType(request.sourceType() == null ? ExcelBookSourceType.SNAPSHOT : request.sourceType());
        entity.setSourceName(request.sourceName());
        entity.setTemplateSheetName(
                request.templateSheetName() == null || request.templateSheetName().isBlank()
                        ? "TEMPLATE"
                        : request.templateSheetName()
        );
        entity.setActiveFlag(request.activeFlag() == null ? true : request.activeFlag());
    }
}