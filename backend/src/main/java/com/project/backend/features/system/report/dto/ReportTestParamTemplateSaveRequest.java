package com.project.backend.features.system.report.dto;

public record ReportTestParamTemplateSaveRequest(
        String reportCode,
        String templateName,
        String jsonText,
        Boolean defaultFlag,
        Boolean activeFlag
) {
}