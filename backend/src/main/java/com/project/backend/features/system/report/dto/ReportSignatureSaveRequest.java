package com.project.backend.features.system.report.dto;

import com.project.backend.features.system.report.enums.ReportSignatureType;

public record ReportSignatureSaveRequest(
        Long reportMasterId,
        ReportSignatureType signatureType,
        String signatureName,
        String fileName,
        String contentType,
        String signatureImageBase64,
        Integer width,
        Integer height,
        Integer displayOrder,
        Boolean defaultFlag,
        Boolean activeFlag
) {
}