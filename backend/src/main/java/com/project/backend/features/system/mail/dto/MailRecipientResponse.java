package com.project.backend.features.system.mail.dto;

import com.project.backend.features.system.mail.enums.MailRecipientType;

import lombok.Builder;

@Builder
public record MailRecipientResponse(
        Long id,
        String recipientKey,
        String recipientName,
        String email,
        MailRecipientType recipientType,
        boolean activeFlag
) {
}