package com.project.backend.features.system.mail.dto;

import java.util.List;

public record MailTestSendRequest(
        List<String> toAddresses,
        String toName,

        List<String> ccAddresses,
        List<String> bccAddresses,

        String subject,
        String body,

        boolean htmlFlag,

        List<MailAttachmentRequest> attachments
) {

    public List<String> toAddressesOrEmpty() {
        return toAddresses != null ? toAddresses : List.of();
    }

    public List<String> ccAddressesOrEmpty() {
        return ccAddresses != null ? ccAddresses : List.of();
    }

    public List<String> bccAddressesOrEmpty() {
        return bccAddresses != null ? bccAddresses : List.of();
    }

    public List<MailAttachmentRequest> attachmentsOrEmpty() {
        return attachments != null ? attachments : List.of();
    }

    public String toNameOrNull() {
        return org.springframework.util.StringUtils.hasText(toName)
                ? toName
                : null;
    }
}