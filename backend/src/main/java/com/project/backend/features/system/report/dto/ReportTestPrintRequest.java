package com.project.backend.features.system.report.dto;

import java.util.Map;

public record ReportTestPrintRequest(
        String reportCode,
        Map<String, Object> params
) {
}