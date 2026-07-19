package com.project.backend.features.system.report.dto;

public record FileExportResult(
        String fileName,
        String contentType,
        byte[] data
) {
}