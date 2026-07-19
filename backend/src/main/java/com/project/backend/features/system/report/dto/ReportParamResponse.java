package com.project.backend.features.system.report.dto;

import com.project.backend.features.system.report.enums.ReportParamControlType;
import com.project.backend.features.system.report.enums.ReportParamType;

import lombok.Builder;

@Builder
public record ReportParamResponse(
        Long id,
        Long reportMasterId,
        String paramName,
        String paramLabel,
        ReportParamType paramType,
        ReportParamControlType controlType,
        Boolean requiredFlag,
        Boolean visibleFlag,
        Boolean multipleFlag,
        Boolean filterFlag,
        String defaultValue,
        String placeholder,
        String inputColumnName,
        Integer displayOrder,
        Boolean activeFlag
) {
}