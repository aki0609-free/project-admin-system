package com.project.backend.features.system.report.service.builder;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.mail.dto.MailAttachmentRequest;
import com.project.backend.features.system.mail.enums.MailStorageType;
import com.project.backend.features.system.report.entity.ReportOutputFile;

@Component
public class ReportMailAttachmentBuilder {

    public MailAttachmentRequest build(ReportOutputFile file) {
        return MailAttachmentRequest.builder()
                .storageType(MailStorageType.valueOf(file.getStorageType().toString()))
                .fileKey(file.getFileKey())
                .fileName(file.getFileName())
                .mimeType(file.getContentType())
                .fileSize(file.getFileSize())
                .build();
    }
}