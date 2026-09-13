package com.project.backend.features.admin.business.dto;

import com.project.backend.features.operation.reportpreview.enums.OperationReportOutputType;
import com.project.backend.features.operation.reportpreview.enums.OperationType;

public record PreviewReportSettingResponse(
        Long id,
        OperationType operationType,
        String reportCode,
        String reportName,
        String tableName,
        String filterColumnName,
        String targetParamName,
        String orderBy,
        Integer displayOrder,
        OperationReportOutputType outputType,
        Boolean activeFlag,
        String htmlTemplateKey,
        Integer htmlTemplateVersion,
        String htmlTemplateHash,
        boolean templateExists
) {
}
