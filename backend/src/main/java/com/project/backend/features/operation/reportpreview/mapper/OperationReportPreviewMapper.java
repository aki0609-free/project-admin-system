package com.project.backend.features.operation.reportpreview.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.operation.reportpreview.dto.OperationReportPreviewColumnResponse;
import com.project.backend.features.operation.reportpreview.dto.OperationReportPreviewResponse;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreviewColumn;
import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.system.report.entity.ReportMaster;

@Component
public class OperationReportPreviewMapper {

    public OperationReportPreviewResponse toResponse(
            OperationReportPreview entity,
            ReportMaster reportMaster,
            List<OperationReportPreviewColumn> columns
    ) {
        return OperationReportPreviewResponse.builder()
                .id(entity.getId())
                .operationType(entity.getOperationType())
                .reportCode(entity.getReportCode())
                .reportName(reportMaster != null ? reportMaster.getReportName() : entity.getReportCode())
                .jobCode(entity.getJobCode())
                .tableName(entity.getTableName())
                .templateName(entity.getTemplateName())
                .displayOrder(entity.getDisplayOrder())
                .outputType(entity.getOutputType() == null
                        ? OperationReportOutputType.NONE
                        : entity.getOutputType())
                .columns(columns.stream()
                        .map(this::toColumnResponse)
                        .toList())
                .build();
    }

    private OperationReportPreviewColumnResponse toColumnResponse(
            OperationReportPreviewColumn entity
    ) {
        return OperationReportPreviewColumnResponse.builder()
                .id(entity.getId())
                .previewName(entity.getPreviewName())
                .columnName(entity.getColumnName())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}