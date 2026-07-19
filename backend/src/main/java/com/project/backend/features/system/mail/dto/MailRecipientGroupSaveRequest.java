package com.project.backend.features.system.mail.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record MailRecipientGroupSaveRequest(
        String groupKey,
        String groupName,
        boolean activeFlag,
        List<MailRecipientSaveRequest> recipients
) {

    public List<MailRecipientSaveRequest> recipientsOrEmpty() {
        return recipients != null
                ? recipients
                : List.of();
    }
}