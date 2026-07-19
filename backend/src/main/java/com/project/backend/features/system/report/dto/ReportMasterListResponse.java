package com.project.backend.features.system.report.dto;

import com.project.backend.features.system.report.enums.ReportOutputFormat;
import com.project.backend.features.system.report.enums.ReportPreProcessType;

import lombok.Builder;

@Builder
public record ReportMasterListResponse(
        Long id,
        String reportCode,
        String reportName,
        String templateFileName,
        String workTable,
        String inputTable,
        String outputTable,
        ReportPreProcessType preProcessType,
        String preProcessSql,
        String procedureName,
        ReportOutputFormat outputFormat,
        Boolean useSignature,
        Boolean previewEnabled,
        Boolean activeFlag
) {
}