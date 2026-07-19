package com.project.backend.features.system.mail.service.builder;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.mail.dto.MailAttachmentRequest;
import com.project.backend.features.system.mail.dto.MailPdfSendRequest;
import com.project.backend.features.system.mail.dto.MailQueueCreateRequest;
import com.project.backend.features.system.mail.enums.MailStorageType;
import com.project.backend.features.system.mail.properties.MailProperties;

@Component
public class MailPdfQueueCreateRequestBuilder {

    public MailQueueCreateRequest build(
            MailPdfSendRequest request,
            MailProperties properties
    ) {
        return MailQueueCreateRequest.builder()
                .mailType(request.mailType())
                .businessKey(request.businessKey())
                .fromAddress(properties.getFromAddress())
                .fromName(properties.getFromName())
                .toAddresses(request.toAddresses())
                .toName(request.toName())
                .ccAddresses(request.ccAddresses())
                .bccAddresses(request.bccAddresses())
                .subject(request.subject())
                .body(request.body())
                .htmlFlag(request.htmlFlag())
                .attachments(List.of(
                        MailAttachmentRequest.builder()
                                .storageType(resolveStorageType(request))
                                .fileKey(request.pdfFileKey())
                                .fileName(request.pdfFileName())
                                .mimeType("application/pdf")
                                .build()
                ))
                .build();
    }

    private MailStorageType resolveStorageType(MailPdfSendRequest request) {
        return request.storageType() != null
                ? request.storageType()
                : MailStorageType.LOCAL;
    }
}