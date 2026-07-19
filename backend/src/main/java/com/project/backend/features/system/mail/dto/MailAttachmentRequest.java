package com.project.backend.features.system.mail.dto;

import com.project.backend.features.system.mail.enums.MailStorageType;

import lombok.Builder;

@Builder
public record MailAttachmentRequest(
        MailStorageType storageType,
        String fileKey,
        String fileName,
        Long fileSize,
        String mimeType
) {
}