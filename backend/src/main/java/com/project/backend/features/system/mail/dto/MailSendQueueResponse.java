package com.project.backend.features.system.mail.dto;

import java.time.Instant;
import java.util.List;

import com.project.backend.features.system.mail.enums.MailSendStatus;

import lombok.Builder;

@Builder
public record MailSendQueueResponse(
        Long id,
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

        List<MailAttachmentResponse> attachments,

        MailSendStatus status,
        int retryCount,
        int maxRetryCount,
        Instant sentAt,
        String lastErrorMessage
) {
}