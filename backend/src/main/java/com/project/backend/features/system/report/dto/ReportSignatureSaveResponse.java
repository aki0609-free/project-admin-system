package com.project.backend.features.system.report.dto;

import lombok.Builder;

@Builder
public record ReportSignatureSaveResponse(
        Long id,
        String message
) {
}