package com.project.backend.features.system.mail.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.features.system.mail.dto.MailPdfSendRequest;
import com.project.backend.features.system.mail.enums.MailStorageType;
import com.project.backend.features.system.mail.properties.MailProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailPdfSendValidator {

    private final StorageService storageService;

    public void validate(MailPdfSendRequest request, MailProperties properties) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        if (properties == null || !StringUtils.hasText(properties.getFromAddress())) {
            throw new RuntimeException("project.mail.from-address が設定されていません。");
        }

        if (!request.hasDirectToAddresses() && !request.hasRecipientGroup()) {
            throw new RuntimeException("toAddresses または recipientGroupKey は必須です。");
        }

        if (!StringUtils.hasText(request.subject())) {
            throw new RuntimeException("subject は必須です。");
        }

        if (!StringUtils.hasText(request.body())) {
            throw new RuntimeException("body は必須です。");
        }

        validatePdf(request);
    }

    private void validatePdf(MailPdfSendRequest request) {
        if (!StringUtils.hasText(request.pdfFileKey())) {
            throw new RuntimeException("pdfFileKey は必須です。");
        }

        if (!StringUtils.hasText(request.pdfFileName())) {
            throw new RuntimeException("pdfFileName は必須です。");
        }

        if (request.storageType() == null) {
            throw new RuntimeException("storageType は必須です。");
        }

        if (!storageService.exists(toStorageType(request.storageType()), request.pdfFileKey())) {
            throw new RuntimeException(
                    "PDFファイルが存在しません。 storageType="
                            + request.storageType()
                            + ", fileKey="
                            + request.pdfFileKey()
            );
        }
    }

    private StorageType toStorageType(MailStorageType storageType) {
        return switch (storageType) {
            case LOCAL -> StorageType.LOCAL;
            case S3 -> StorageType.S3;
        };
    }
}
