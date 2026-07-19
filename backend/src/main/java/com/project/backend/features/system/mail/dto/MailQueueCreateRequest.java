package com.project.backend.features.system.mail.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record MailQueueCreateRequest(
        String mailType,
        String businessKey,

        String fromAddress,
        String fromName,

        List<String> toAddresses,
        String toName,
        List<String> ccAddresses,
        List<String> bccAddresses,

        String subject,
        String body,
        boolean htmlFlag,

        List<MailAttachmentRequest> attachments
) {

    public List<MailAttachmentRequest> attachmentsOrEmpty() {
        return attachments != null ? attachments : List.of();
    }
}