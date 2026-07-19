package com.project.backend.features.operation.reportpreview.dto;

import lombok.Builder;

@Builder
public record OperationReportPreviewColumnResponse(
        Long id,
        String previewName,
        String columnName,
        Integer displayOrder
) {
}