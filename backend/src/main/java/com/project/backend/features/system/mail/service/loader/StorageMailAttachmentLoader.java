package com.project.backend.features.system.mail.service.loader;

import java.io.InputStream;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import com.project.backend.app.storage.enums.StorageType;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.mail.dto.MailAttachmentResource;
import com.project.backend.features.system.mail.entity.MailSendAttachment;
import com.project.backend.features.system.mail.enums.MailStorageType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StorageMailAttachmentLoader implements MailAttachmentLoader {

    private final StorageService storageService;

    @Override
    public boolean exists(MailSendAttachment attachment) {
        validateAttachment(attachment);

        return storageService.exists(
                toStorageType(attachment.getStorageType()),
                attachment.getFileKey()
        );
    }

    @Override
    public MailAttachmentResource load(MailSendAttachment attachment) {
        validateAttachment(attachment);

        try (InputStream inputStream = storageService.load(
                toStorageType(attachment.getStorageType()),
                attachment.getFileKey()
        )) {
            byte[] data = inputStream.readAllBytes();

            return new MailAttachmentResource(
                    attachment.getFileName(),
                    attachment.getMimeType(),
                    new ByteArrayResource(data)
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "メール添付ファイルの読込に失敗しました。 storageType="
                            + attachment.getStorageType(),
                    e
            );
        }
    }

    private void validateAttachment(MailSendAttachment attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("メール添付情報が未設定です。");
        }

        if (attachment.getStorageType() == null) {
            throw new IllegalArgumentException("メール添付のstorageTypeが未設定です。");
        }
    }

    private StorageType toStorageType(MailStorageType storageType) {
        return switch (storageType) {
            case LOCAL -> StorageType.LOCAL;
            case S3 -> StorageType.S3;
        };
    }
}
