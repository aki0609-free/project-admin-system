package com.project.backend.features.system.imports.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.imports.dto.ImportColumnDefinition;
import com.project.backend.features.system.imports.dto.ImportColumnSaveRequest;
import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.dto.ImportTargetSaveRequest;
import com.project.backend.features.system.imports.dto.ImportTargetSummary;
import com.project.backend.features.system.imports.entity.ImportColumn;
import com.project.backend.features.system.imports.entity.ImportTarget;
import com.project.backend.features.system.imports.enums.ImportMode;
import com.project.backend.features.system.imports.enums.ImportScriptType;
import com.project.backend.features.system.imports.enums.ImportSourceType;

@Mapper(componentModel = "spring")
public interface ImportTargetMapper {

    ImportTargetSummary toSummary(ImportTarget entity);

    List<ImportTargetSummary> toSummaryList(List<ImportTarget> entities);

    @Mapping(target = "columns", expression = "java(toActiveColumnDefinitions(entity.getColumns()))")
    ImportTargetDefinition toDefinition(ImportTarget entity);

    ImportColumnDefinition toColumnDefinition(ImportColumn entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "columns", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "sourceType", expression = "java(resolveSourceType(request))")
    @Mapping(target = "fixedFilePath", expression = "java(normalizeBlank(request.fixedFilePath()))")
    @Mapping(target = "scriptType", expression = "java(resolveScriptType(request))")
    @Mapping(target = "scriptPath", expression = "java(normalizeBlank(request.scriptPath()))")
    @Mapping(target = "scriptArgs", expression = "java(normalizeBlank(request.scriptArgs()))")
    @Mapping(target = "importMode", expression = "java(resolveImportMode(request))")
    @Mapping(target = "headerRowNumber", expression = "java(request.headerRowNumber() != null ? request.headerRowNumber() : 1)")
    @Mapping(target = "dataStartRowNumber", expression = "java(request.dataStartRowNumber() != null ? request.dataStartRowNumber() : 2)")
    @Mapping(target = "charset", expression = "java(normalizeBlankOrDefault(request.charset(), \"UTF-8\"))")
    @Mapping(target = "delimiter", expression = "java(normalizeBlankOrDefault(request.delimiter(), \",\"))")
    @Mapping(target = "activeFlag", expression = "java(request.activeFlagOrDefault())")
    void updateTargetFromRequest(
            ImportTargetSaveRequest request,
            @MappingTarget ImportTarget entity
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "target", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "columnName", expression = "java(normalizeBlank(request.columnName()))")
    @Mapping(target = "csvHeaderName", expression = "java(normalizeBlank(request.csvHeaderName()))")
    @Mapping(target = "dataType", source = "dataType")
    @Mapping(target = "requiredFlag", expression = "java(request.requiredFlagOrDefault())")
    @Mapping(target = "keyFlag", expression = "java(request.keyFlagOrDefault())")
    @Mapping(target = "nullableFlag", expression = "java(request.nullableFlagOrDefault())")
    @Mapping(target = "trimFlag", expression = "java(request.trimFlagOrDefault())")
    @Mapping(target = "defaultValue", expression = "java(normalizeBlank(request.defaultValue()))")
    @Mapping(target = "formatPattern", expression = "java(normalizeBlank(request.formatPattern()))")
    @Mapping(target = "updatableFlag", expression = "java(resolveUpdatableFlag(request))")
    @Mapping(target = "orderNo", expression = "java(request.orderNoOrDefault())")
    void updateColumnFromRequest(
            ImportColumnSaveRequest request,
            @MappingTarget ImportColumn entity
    );

    default List<ImportColumnDefinition> toActiveColumnDefinitions(
            List<ImportColumn> columns
    ) {
        if (columns == null) {
            return List.of();
        }

        return columns.stream()
                .filter(column -> column.getDeletedAt() == null)
                .sorted((a, b) -> Integer.compare(a.getOrderNo(), b.getOrderNo()))
                .map(this::toColumnDefinition)
                .toList();
    }

    default ImportSourceType resolveSourceType(
            ImportTargetSaveRequest request
    ) {
        return request.sourceType() != null
                ? request.sourceType()
                : ImportSourceType.UPLOAD;
    }

    default ImportScriptType resolveScriptType(
            ImportTargetSaveRequest request
    ) {
        return request.scriptType() != null
                ? request.scriptType()
                : ImportScriptType.NONE;
    }

    default ImportMode resolveImportMode(
            ImportTargetSaveRequest request
    ) {
        return request.importMode() != null
                ? request.importMode()
                : ImportMode.INSERT_ONLY;
    }

    default boolean resolveUpdatableFlag(
            ImportColumnSaveRequest request
    ) {
        if (request.keyFlagOrDefault()) {
            return false;
        }

        return request.updatableFlagOrDefault();
    }

    default String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    default String normalizeBlankOrDefault(
            String value,
            String defaultValue
    ) {
        return StringUtils.hasText(value)
                ? value.trim()
                : defaultValue;
    }
}