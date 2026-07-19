package com.project.backend.features.operation.reportpreview.dto;

import java.util.List;

import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.operation.reportpreview.enums.OperationType;

import lombok.Builder;

@Builder
public record OperationReportPreviewResponse(
        Long id,
        OperationType operationType,
        String reportCode,
        String reportName,
        String jobCode,
        String tableName,
        String templateName,
        Integer displayOrder,
        OperationReportOutputType outputType,
        List<OperationReportPreviewColumnResponse> columns
) {
}