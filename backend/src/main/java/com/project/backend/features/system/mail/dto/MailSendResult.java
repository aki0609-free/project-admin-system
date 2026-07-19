package com.project.backend.features.system.mail.dto;

import lombok.Builder;

@Builder
public record MailSendResult(
        int targetCount,
        int sentCount,
        int failedCount,
        String message
) {
}