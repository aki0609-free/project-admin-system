package com.project.backend.features.operation.monthly.dto;

import java.time.Instant;

public record MonthlyClosingReportFileResponse(
        Long id,
        String reportCode,
        String targetType,
        Long targetId,
        String targetName,
        Long batchExecutionLogId,
        String storageType,
        String outputFileKey,
        String outputFileName,
        String contentType,
        Long fileSize,
        Instant generatedAt
) {
}
