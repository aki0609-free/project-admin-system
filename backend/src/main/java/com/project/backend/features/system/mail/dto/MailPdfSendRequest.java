package com.project.backend.features.system.mail.dto;

import java.util.List;

import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.enums.MailStorageType;

public record MailPdfSendRequest(
        String mailType,
        String businessKey,
        String recipientGroupKey,
        String recipientKey,
        List<String> toAddresses,
        String toName,
        List<String> ccAddresses,
        List<String> bccAddresses,
        String subject,
        String body,
        boolean htmlFlag,
        MailStorageType storageType,
        String pdfFileKey,
        String pdfFileName
) {

    public boolean hasRecipientGroup() {
        return StringUtils.hasText(recipientGroupKey);
    }

    public boolean hasDirectToAddresses() {
        return toAddresses != null
                && toAddresses.stream().anyMatch(StringUtils::hasText);
    }
}