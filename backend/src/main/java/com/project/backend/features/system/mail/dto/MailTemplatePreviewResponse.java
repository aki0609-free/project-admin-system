package com.project.backend.features.system.mail.dto;

public record MailTemplatePreviewResponse(
        String subject,
        String body,
        boolean htmlFlag
) {
}
