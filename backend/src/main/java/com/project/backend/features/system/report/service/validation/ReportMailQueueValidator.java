package com.project.backend.features.system.report.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.report.entity.ReportOutputFile;

@Component
public class ReportMailQueueValidator {

    public void validate(ReportOutputFile file) {
        if (file == null) {
            throw new RuntimeException("ReportOutputFile が未設定です。");
        }

        if (!StringUtils.hasText(file.getMailTemplateKey())) {
            throw new RuntimeException("mailTemplateKey が空です。 reportOutputFileId=" + file.getId());
        }

        if (!StringUtils.hasText(file.getRecipientEmail())
                && !StringUtils.hasText(file.getRecipientGroupKey())) {
            throw new RuntimeException(
                    "recipientEmail または recipientGroupKey が必要です。 reportOutputFileId=" + file.getId());
        }

        if (file.getStorageType() == null) {
            throw new RuntimeException("storageType が空です。 reportOutputFileId=" + file.getId());
        }

        if (!StringUtils.hasText(file.getFileKey())) {
            throw new RuntimeException("fileKey が空です。 reportOutputFileId=" + file.getId());
        }

        if (!StringUtils.hasText(file.getFileName())) {
            throw new RuntimeException("fileName が空です。 reportOutputFileId=" + file.getId());
        }
    }
}