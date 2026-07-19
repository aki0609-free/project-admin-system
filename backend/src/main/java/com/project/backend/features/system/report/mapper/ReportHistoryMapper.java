package com.project.backend.features.system.report.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.project.backend.features.system.report.dto.ReportHistoryDetailResponse;
import com.project.backend.features.system.report.dto.ReportHistoryResponse;
import com.project.backend.features.system.report.entity.ReportHistory;

@Mapper(componentModel = "spring")
public interface ReportHistoryMapper {

    @Mapping(target = "reportMasterId", source = "reportMaster.id")
    @Mapping(target = "reportCode", source = "reportMaster.reportCode")
    @Mapping(target = "reportName", source = "reportMaster.reportName")
    ReportHistoryResponse toResponse(ReportHistory entity);

    List<ReportHistoryResponse> toResponseList(List<ReportHistory> entities);

    @Mapping(target = "reportMasterId", source = "reportMaster.id")
    @Mapping(target = "reportCode", source = "reportMaster.reportCode")
    @Mapping(target = "reportName", source = "reportMaster.reportName")
    ReportHistoryDetailResponse toDetailResponse(ReportHistory entity);
}