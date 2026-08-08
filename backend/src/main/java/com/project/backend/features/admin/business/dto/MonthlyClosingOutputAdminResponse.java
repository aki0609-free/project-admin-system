package com.project.backend.features.admin.business.dto;

import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;

public record MonthlyClosingOutputAdminResponse(
        Long id,
        String reportCode,
        String reportName,
        String jobCode,
        OperationReportOutputType outputType,
        int executionOrder,
        boolean requiredFlag,
        boolean activeFlag,
        Integer backupRetentionYears
) {
}
