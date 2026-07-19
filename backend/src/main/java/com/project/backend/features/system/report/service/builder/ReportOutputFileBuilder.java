package com.project.backend.features.system.report.service.builder;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.common.util.ApplicationStringUtils;
import com.project.backend.features.system.report.dto.ReportStoredFile;
import com.project.backend.features.system.report.dto.ReportRecipientOutputGroup;
import com.project.backend.features.system.report.entity.ReportMaster;
import com.project.backend.features.system.report.entity.ReportOutputFile;
import com.project.backend.features.system.report.enums.ReportOutputFileStatus;

@Component
public class ReportOutputFileBuilder {

    public ReportOutputFile build(
            ReportMaster reportMaster,
            String executionId,
            ReportStoredFile storedFile,
            ReportRecipientOutputGroup group
    ) {
        ReportOutputFile file = new ReportOutputFile();

        file.setExecutionId(executionId);
        file.setReportCode(reportMaster.getReportCode());
        file.setBusinessKey(group.businessKey());
        file.setRecipientKey(group.recipientKey());
        file.setRecipientName(group.recipientName());
        file.setRecipientEmail(group.recipientEmail());
        file.setRecipientGroupKey(group.recipientGroupKey());
        file.setStorageType(storedFile.storageType());
        file.setFileKey(storedFile.fileKey());
        file.setFileName(storedFile.fileName());
        file.setContentType(storedFile.contentType());
        file.setFileSize(storedFile.fileSize());
        file.setMailType(group.mailType());
        file.setMailTemplateKey(group.mailTemplateKey());

        boolean hasRecipient =
                StringUtils.hasText(group.recipientEmail())
                        || StringUtils.hasText(group.recipientGroupKey());

        if (hasRecipient) {
            file.setStatus(ReportOutputFileStatus.CREATED);
            file.setErrorMessage(null);
        } else {
            file.setStatus(ReportOutputFileStatus.FAILED);
            file.setErrorMessage("宛先メールアドレスが未設定です。");
        }

        return file;
    }

    public ReportOutputFile build(
            ReportMaster reportMaster,
            String executionId,
            ReportStoredFile storedFile,
            Map<String,Object> row
    ) {

        String mailTemplateKey =
                value(row, "mail_template_key");

        String recipientEmail =
                value(row, "recipient_email");

        String recipientGroupKey =
                value(row, "recipient_group_key");

        boolean hasMailInfo =
                StringUtils.hasText(mailTemplateKey)
                        && (
                        StringUtils.hasText(recipientEmail)
                                || StringUtils.hasText(recipientGroupKey)
                );

        if (!hasMailInfo) {
            return null;
        }

        ReportOutputFile file =
                new ReportOutputFile();

        file.setExecutionId(executionId);
        file.setReportCode(reportMaster.getReportCode());

        file.setBusinessKey(
                defaultValue(
                        value(row, "business_key"),
                        reportMaster.getReportCode()
                                + ":"
                                + executionId
                )
        );

        file.setRecipientKey(
                value(row, "recipient_key")
        );

        file.setRecipientName(
                value(row, "recipient_name")
        );

        file.setRecipientEmail(
                recipientEmail
        );

        file.setRecipientGroupKey(
                recipientGroupKey
        );

        file.setStorageType(
                storedFile.storageType()
        );

        file.setFileKey(
                storedFile.fileKey()
        );

        file.setFileName(
                storedFile.fileName()
        );

        file.setContentType(
                storedFile.contentType()
        );

        file.setFileSize(
                storedFile.fileSize()
        );

        file.setMailType(
                defaultValue(
                        value(row, "mail_type"),
                        reportMaster.getReportCode()
                )
        );

        file.setMailTemplateKey(
                mailTemplateKey
        );

        file.setStatus(
                ReportOutputFileStatus.CREATED
        );

        return file;
    }

    private String value(
            Map<String,Object> row,
            String key
    ) {
        Object value = row.get(key);

        if (value == null) {
            value = row.get(
                    ApplicationStringUtils.toCamelCase(key)
            );
        }

        return value != null
                ? String.valueOf(value)
                : null;
    }

    private String defaultValue(
            String value,
            String defaultValue
    ) {
        return StringUtils.hasText(value)
                ? value
                : defaultValue;
    }
}
