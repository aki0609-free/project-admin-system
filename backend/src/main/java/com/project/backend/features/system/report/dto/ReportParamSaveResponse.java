package com.project.backend.features.system.report.dto;

import lombok.Builder;

@Builder
public record ReportParamSaveResponse(
        Long id,
        String message
) {
}