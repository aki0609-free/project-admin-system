package com.project.backend.features.system.report.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.dto.ReportSignatureSaveRequest;

@Component
public class ReportSignatureValidator {

    public void validateSaveRequest(ReportSignatureSaveRequest request, Long id) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (request.reportMasterId() == null) {
            throw new RuntimeException("reportMasterId は必須です。");
        }

        if (!StringUtils.hasText(request.signatureName())) {
            throw new RuntimeException("signatureName は必須です。");
        }

        if (request.signatureType() == null) {
            throw new RuntimeException("signatureType は必須です。");
        }

        if (request.displayOrder() == null || request.displayOrder() < 1) {
            throw new RuntimeException("displayOrder は1以上で指定してください。");
        }

        if (id == null && !StringUtils.hasText(request.signatureImageBase64())) {
            throw new RuntimeException("新規作成時は署名画像が必須です。");
        }
    }
}