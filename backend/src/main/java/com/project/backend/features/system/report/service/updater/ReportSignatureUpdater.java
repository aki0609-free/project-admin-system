package com.project.backend.features.system.report.service.updater;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.common.util.ApplicationBase64Utils;
import com.project.backend.features.system.report.dto.ReportSignatureSaveRequest;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportSignature;
import com.project.backend.features.system.report.enums.ReportSignatureType;

@Component
public class ReportSignatureUpdater {

    public void apply(
            ReportSignature entity,
            ReportSignatureSaveRequest request,
            ReportMaster reportMaster,
            boolean create
    ) {
        entity.setReportMaster(reportMaster);
        entity.setSignatureType(
                request.signatureType() != null ? request.signatureType() : ReportSignatureType.STAMP
        );
        entity.setSignatureName(request.signatureName().trim());
        entity.setFileName(request.fileName());
        entity.setContentType(request.contentType());
        entity.setWidth(request.width());
        entity.setHeight(request.height());
        entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 1);
        entity.setDefaultFlag(request.defaultFlag() != null ? request.defaultFlag() : false);
        entity.setActiveFlag(request.activeFlag() != null ? request.activeFlag() : true);

        if (StringUtils.hasText(request.signatureImageBase64())) {
            entity.setSignatureImageData(
                    ApplicationBase64Utils.decodeDataUrlOrBase64(request.signatureImageBase64())
            );
        } else if (create && entity.getSignatureImageData() == null) {
            throw new RuntimeException("署名画像が必須です。");
        }
    }
}