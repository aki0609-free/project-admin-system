package com.project.backend.features.system.report.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.project.backend.features.system.report.dto.ReportTestParamTemplateResponse;
import com.project.backend.features.system.report.dto.ReportTestParamTemplateSaveRequest;
import com.project.backend.features.system.report.entity.ReportTestParamTemplate;

@Mapper(componentModel = "spring")
public interface ReportTestParamTemplateMapper {

    ReportTestParamTemplateResponse toResponse(ReportTestParamTemplate entity);

    List<ReportTestParamTemplateResponse> toResponseList(List<ReportTestParamTemplate> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void updateEntityFromRequest(
            ReportTestParamTemplateSaveRequest request,
            @MappingTarget ReportTestParamTemplate entity
    );
}