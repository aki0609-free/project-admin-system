package com.project.backend.features.system.mail.dto;

import org.springframework.util.StringUtils;

public record MailActivateDraftRequest(
        String mailType,
        String businessKey
) {

    public boolean hasMailType() {
        return StringUtils.hasText(mailType);
    }

    public boolean hasBusinessKey() {
        return StringUtils.hasText(businessKey);
    }
}