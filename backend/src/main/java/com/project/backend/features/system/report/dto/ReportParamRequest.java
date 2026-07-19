package com.project.backend.features.system.report.dto;

import com.project.backend.features.system.report.enums.ReportParamControlType;
import com.project.backend.features.system.report.enums.ReportParamType;

public record ReportParamRequest(
        Long id,
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