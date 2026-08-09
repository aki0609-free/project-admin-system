package com.project.backend.features.operation.monthly.dto;

import com.project.backend.features.operation.monthly.enums.AnnualReportBackupStatus;

public record AnnualReportBackupResult(
        Long executionId,
        Integer fiscalYear,
        AnnualReportBackupStatus status,
        Integer fileCount,
        Long totalSize,
        String errorMessage
) {
}
