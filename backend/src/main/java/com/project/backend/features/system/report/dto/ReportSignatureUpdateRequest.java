package com.project.backend.features.system.report.dto;

import com.project.backend.features.system.report.enums.ReportSignatureType;

public record ReportSignatureUpdateRequest(
        ReportSignatureType signatureType,
        String signatureName,
        Integer width,
        Integer height,
        Integer displayOrder,
        Boolean defaultFlag,
        Boolean activeFlag
) {
}