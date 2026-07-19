package com.project.backend.features.system.report.dto;

import java.util.ArrayList;
import java.util.List;

import com.project.backend.features.system.report.enums.ReportCleanupType;
import com.project.backend.features.system.report.enums.ReportLayoutType;
import com.project.backend.features.system.report.enums.ReportOutputFormat;
import com.project.backend.features.system.report.enums.ReportPreProcessType;

public record ReportMasterSaveRequest(
        String reportCode,
        String reportName,
        String templateFileName,
        String workTable,
        String inputTable,
        String outputTable,
        ReportPreProcessType preProcessType,
        String preProcessSql,
        String procedureName,
        String querySql,
        ReportCleanupType cleanupType,
        String cleanupSql,
        String cleanupProcedureName,
        ReportLayoutType layoutType,
        Integer layoutCount,
        String fileName,
        ReportOutputFormat outputFormat,
        Boolean useSignature,
        Boolean previewEnabled,
        Boolean activeFlag,
        List<ReportParamRequest> params
) {
    public ReportMasterSaveRequest {
        params = params != null ? params : new ArrayList<>();
    }
}