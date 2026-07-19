package com.project.backend.features.system.mail.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record MailRecipientGroupResponse(
        Long id,
        String groupKey,
        String groupName,
        boolean activeFlag,
        List<MailRecipientResponse> recipients
) {
}