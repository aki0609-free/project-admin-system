package com.project.backend.features.system.report.mapper;

import java.util.List;

import org.mapstruct.*;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.dto.*;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportParam;

@Mapper(componentModel = "spring")
public interface ReportMasterDtoMapper {

    ReportMasterListResponse toListResponse(ReportMaster entity);

    @Mapping(target = "params", source = "params")
    ReportMasterDetailResponse toDetailResponse(ReportMaster entity);

    List<ReportMasterListResponse> toListResponseList(List<ReportMaster> entities);

    @Mapping(target = "reportMasterId", source = "reportMaster.id")
    ReportParamResponse toParamResponse(ReportParam entity);

    List<ReportParamResponse> toParamResponseList(List<ReportParam> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "params", ignore = true)
    @Mapping(target = "histories", ignore = true)
    @Mapping(target = "signatures", ignore = true)
    void updateMasterFromRequest(
            ReportMasterSaveRequest request,
            @MappingTarget ReportMaster entity
    );

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "reportMaster", ignore = true)
    void updateParamFromRequest(
            ReportParamRequest request,
            @MappingTarget ReportParam entity
    );

    @AfterMapping
    default void normalizeMaster(@MappingTarget ReportMaster entity) {
        if (!StringUtils.hasText(entity.getInputTable())) {
            entity.setInputTable(null);
        }
        if (!StringUtils.hasText(entity.getOutputTable())) {
            entity.setOutputTable(null);
        }
        if (!StringUtils.hasText(entity.getTemplateFileName())) {
            entity.setTemplateFileName(null);
        }
        if (!StringUtils.hasText(entity.getProcedureName())) {
            entity.setProcedureName(null);
        }
        if (!StringUtils.hasText(entity.getPreProcessSql())) {
            entity.setPreProcessSql(null);
        }
        if (!StringUtils.hasText(entity.getQuerySql())) {
            entity.setQuerySql(null);
        }
        if (!StringUtils.hasText(entity.getCleanupSql())) {
            entity.setCleanupSql(null);
        }
        if (!StringUtils.hasText(entity.getCleanupProcedureName())) {
            entity.setCleanupProcedureName(null);
        }
        if (!StringUtils.hasText(entity.getFileName())) {
            entity.setFileName(null);
        }
    }

    @AfterMapping
    default void normalizeParam(@MappingTarget ReportParam entity) {
        if (!StringUtils.hasText(entity.getDefaultValue())) {
            entity.setDefaultValue(null);
        }
        if (!StringUtils.hasText(entity.getPlaceholder())) {
            entity.setPlaceholder(null);
        }
        if (!StringUtils.hasText(entity.getInputColumnName())) {
            entity.setInputColumnName(null);
        }
    }
}