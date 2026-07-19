package com.project.backend.features.system.mail.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;

@Builder
public record MailTemplateQueueCreateRequest(
        String templateKey,
        String recipientGroupKey,
        String recipientKey,

        String mailType,
        String businessKey,

        List<String> toAddresses,
        String toName,
        List<String> ccAddresses,
        List<String> bccAddresses,

        List<MailAttachmentRequest> attachments,

        Map<String, Object> variables
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

    public Map<String, Object> variablesOrEmpty() {
        return variables != null ? variables : Map.of();
    }
}