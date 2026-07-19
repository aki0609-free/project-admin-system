package com.project.backend.features.system.report.dto;

public record ReportDownloadFile(
        String fileName,
        String mimeType,
        byte[] data
) {
}