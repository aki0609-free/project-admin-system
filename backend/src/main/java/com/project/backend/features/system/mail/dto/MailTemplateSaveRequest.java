package com.project.backend.features.system.mail.dto;

public record MailTemplateSaveRequest(
        String templateKey,
        String templateName,
        String fromName,
        String subjectTemplate,
        String bodyTemplate,
        boolean htmlFlag,
        boolean activeFlag
) {
}
