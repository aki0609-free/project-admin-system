package com.project.backend.features.system.backup.mapper;

import java.util.Comparator;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.backup.dto.BackupColumnDefinition;
import com.project.backend.features.system.backup.dto.BackupColumnSaveRequest;
import com.project.backend.features.system.backup.dto.BackupTargetDefinition;
import com.project.backend.features.system.backup.dto.BackupTargetResponse;
import com.project.backend.features.system.backup.dto.BackupTargetSaveRequest;
import com.project.backend.features.system.backup.dto.BackupTargetSummary;
import com.project.backend.features.system.backup.entity.BackupColumn;
import com.project.backend.features.system.backup.entity.BackupTarget;

@Mapper(componentModel = "spring")
public interface BackupTargetMapper {

    @Mapping(target = "columns", expression = "java(java.util.List.of())")
    BackupTargetResponse toResponse(BackupTargetSummary summary);

    List<BackupTargetResponse> toResponseListFromSummaries(
            List<BackupTargetSummary> summaries
    );

    @Mapping(target = "columns", expression = "java(toSortedColumnDefinitions(entity.getColumns()))")
    BackupTargetResponse toResponse(BackupTarget entity);

    @Mapping(target = "columns", expression = "java(toSortedColumnDefinitions(entity.getColumns()))")
    BackupTargetDefinition toDefinition(BackupTarget entity);

    BackupTargetSummary toSummary(BackupTarget entity);

    BackupColumnDefinition toColumnDefinition(BackupColumn column);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "target", ignore = true)
    @Mapping(target = "exportFlag", expression = "java(request.exportFlag() == null || request.exportFlag())")
    @Mapping(target = "orderNo", expression = "java(request.orderNo() != null ? request.orderNo() : 1)")
    BackupColumn toColumnEntity(BackupColumnSaveRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "columns", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "description", source = "description")
    @Mapping(target = "backupEnabled", expression = "java(request.backupEnabled() == null || request.backupEnabled())")
    @Mapping(target = "activeFlag", expression = "java(request.activeFlag() == null || request.activeFlag())")
    @Mapping(target = "outputMode", expression = "java(request.outputMode() != null ? request.outputMode() : com.project.backend.features.system.backup.enums.BackupOutputMode.DOWNLOAD)")
    @Mapping(target = "outputDir", source = "outputDir", qualifiedByName = "normalizeBlank")
    @Mapping(target = "fileNamePattern", source = "fileNamePattern", qualifiedByName = "normalizeBlank")
    @Mapping(target = "zipRequired", expression = "java(Boolean.TRUE.equals(request.zipRequired()))")
    void updateEntityFromRequest(
            BackupTargetSaveRequest request,
            @MappingTarget BackupTarget entity
    );

    @AfterMapping
    default void afterUpdateEntityFromRequest(
            BackupTargetSaveRequest request,
            @MappingTarget BackupTarget entity
    ) {
        entity.clearColumns();

        List<BackupColumnSaveRequest> columns =
                request.columns() != null
                        ? request.columns()
                        : List.of();

        for (BackupColumnSaveRequest columnRequest : columns) {
            entity.addColumn(toColumnEntity(columnRequest));
        }
    }

    @SuppressWarnings("null")
    default List<BackupColumnDefinition> toSortedColumnDefinitions(
            List<BackupColumn> columns
    ) {
        if (columns == null) {
            return List.of();
        }

        return columns.stream()
                .sorted(Comparator.comparingInt(BackupColumn::getOrderNo))
                .map(this::toColumnDefinition)
                .toList();
    }

    @Named("normalizeBlank")
    default String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}