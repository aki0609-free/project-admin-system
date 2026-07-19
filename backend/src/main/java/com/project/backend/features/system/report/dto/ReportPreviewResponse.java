package com.project.backend.features.system.report.dto;

import lombok.Builder;

@Builder
public record ReportPreviewResponse(
        String reportCode,
        String executionId,
        String contentType,
        String fileName,
        String base64Data
) {
}