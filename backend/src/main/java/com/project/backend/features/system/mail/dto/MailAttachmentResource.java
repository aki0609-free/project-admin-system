package com.project.backend.features.system.mail.dto;

import org.springframework.core.io.InputStreamSource;

public record MailAttachmentResource(
        String fileName,
        String mimeType,
        InputStreamSource source
) {
}