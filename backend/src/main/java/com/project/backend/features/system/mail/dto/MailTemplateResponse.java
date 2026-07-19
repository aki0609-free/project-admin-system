package com.project.backend.features.system.mail.dto;

public record MailTemplateResponse(
        Long id,
        String templateKey,
        String templateName,
        String fromAddress,
        String fromName,
        String subjectTemplate,
        String bodyTemplate,
        boolean htmlFlag,
        boolean activeFlag
) {
}
