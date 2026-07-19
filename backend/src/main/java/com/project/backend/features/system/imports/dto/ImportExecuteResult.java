package com.project.backend.features.system.imports.dto;

import lombok.Builder;

@Builder
public record ImportExecuteResult(
        String targetCode,
        String fileName,
        Long jobExecutionId,
        String status,
        String message
) {
}