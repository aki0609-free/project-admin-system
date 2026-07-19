package com.project.backend.features.system.report.dto;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.report.enums.ReportHistoryStatus;
import com.project.backend.features.system.report.enums.ReportOutputFormat;

import lombok.Builder;

@Builder
public record ReportHistoryResponse(
        Long id,
        Long reportMasterId,
        String reportCode,
        String reportName,
        String fileName,
        ReportOutputFormat outputFormat,
        ReportHistoryStatus status,
        String executedBy,
        String requestParamsJson,
        String notes,
        StorageType storageType,
        String storedFileKey,
        String storedFileName,
        String mimeType,
        Long fileSize,
        String createdAt
) {
}