package com.project.backend.features.operation.reportpreview.dto;

import com.project.backend.features.operation.reportpreview.enums.OperationType;

public record OperationReportPreviewHtmlRequest(
        OperationType operationType,
        String reportCode,
        String targetDate,
        String targetMonth
) {
}