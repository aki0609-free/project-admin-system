package com.project.backend.features.system.report.service.builder;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ReportBatchRequestParamsBuilder {

    public Map<String, Object> build(
            String reportCode,
            String executionId,
            Map<String, Object> params
    ) {
        return Map.of(
                "reportCode", reportCode,
                "executionId", executionId,
                "params", params != null ? params : Map.of()
        );
    }
}