package com.project.backend.features.system.report.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record ReportExecuteRequest(
        Map<String, Object> params
) {
    public ReportExecuteRequest {
        params = params != null ? params : new LinkedHashMap<>();
    }
}