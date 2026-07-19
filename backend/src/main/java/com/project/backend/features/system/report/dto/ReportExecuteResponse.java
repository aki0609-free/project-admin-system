package com.project.backend.features.system.report.dto;

import lombok.Builder;

@Builder
public record ReportExecuteResponse(
        String reportCode,
        String executionId,
        String outputFormat,
        String fileName,
        Boolean previewEnabled,
        String message
) {
}