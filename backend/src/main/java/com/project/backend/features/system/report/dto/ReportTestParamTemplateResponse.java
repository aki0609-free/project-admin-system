package com.project.backend.features.system.report.dto;

import lombok.Builder;

@Builder
public record ReportTestParamTemplateResponse(
        Long id,
        String reportCode,
        String templateName,
        String jsonText,
        Boolean defaultFlag,
        Boolean activeFlag
) {
}