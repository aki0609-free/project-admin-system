package com.project.backend.features.system.mail.dto;

import com.project.backend.features.system.mail.enums.MailRecipientType;

public record MailRecipientSaveRequest(
        Long id,
        String recipientKey,
        String recipientName,
        String email,
        MailRecipientType recipientType,
        boolean activeFlag
) {

    public MailRecipientType recipientTypeOrDefault() {
        return recipientType != null
                ? recipientType
                : MailRecipientType.TO;
    }
}