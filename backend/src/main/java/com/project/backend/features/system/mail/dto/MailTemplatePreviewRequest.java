package com.project.backend.features.system.mail.dto;

import java.util.Map;

public record MailTemplatePreviewRequest(
        String subjectTemplate,
        String bodyTemplate,
        boolean htmlFlag,
        Map<String, Object> variables
) {

    public Map<String, Object> variablesOrEmpty() {
        return variables != null ? variables : Map.of();
    }
}
