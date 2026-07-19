package com.project.backend.features.system.report.dto;

import com.project.backend.features.system.report.enums.ReportSignatureType;

import lombok.Builder;

@Builder
public record ReportSignatureResponse(
        Long id,
        Long reportMasterId,
        ReportSignatureType signatureType,
        String signatureName,
        String fileName,
        String contentType,
        Integer width,
        Integer height,
        Integer displayOrder,
        Boolean defaultFlag,
        Boolean activeFlag,
        String signatureImageBase64
) {
}