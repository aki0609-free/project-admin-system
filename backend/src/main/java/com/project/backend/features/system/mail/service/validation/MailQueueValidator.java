package com.project.backend.features.system.mail.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.entity.MailSendAttachment;
import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.service.loader.MailAttachmentLoader;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailQueueValidator {

    private final MailAttachmentLoader attachmentLoader;

    public void validateBeforeWaiting(MailSendQueue mail) {
        validateCommon(mail);
        validateAttachmentsBasic(mail);
    }

    public void validateBeforeSend(MailSendQueue mail) {
        validateCommon(mail);
        validateAttachmentsBasic(mail);
        validateAttachmentExists(mail);
    }

    private void validateCommon(MailSendQueue mail) {
        if (mail == null) {
            throw new RuntimeException("メールキューが不正です。");
        }

        if (!StringUtils.hasText(mail.getFromAddress())) {
            throw new RuntimeException("fromAddress は必須です。");
        }

        if (!StringUtils.hasText(mail.getToAddress())) {
            throw new RuntimeException("toAddress は必須です。");
        }

        if (!StringUtils.hasText(mail.getSubject())) {
            throw new RuntimeException("subject は必須です。");
        }

        if (!StringUtils.hasText(mail.getBody())) {
            throw new RuntimeException("body は必須です。");
        }
    }

    private void validateAttachmentsBasic(MailSendQueue mail) {
        if (mail.getAttachments() == null) {
            return;
        }

        for (MailSendAttachment attachment : mail.getAttachments()) {
            if (attachment.getStorageType() == null) {
                throw new RuntimeException("attachment.storageType は必須です。");
            }

            if (!StringUtils.hasText(attachment.getFileKey())) {
                throw new RuntimeException("attachment.fileKey は必須です。");
            }

            if (!StringUtils.hasText(attachment.getFileName())) {
                throw new RuntimeException("attachment.fileName は必須です。");
            }
        }
    }

    private void validateAttachmentExists(MailSendQueue mail) {
        if (mail.getAttachments() == null) {
            return;
        }

        for (MailSendAttachment attachment : mail.getAttachments()) {
            if (!attachmentLoader.exists(attachment)) {
                throw new RuntimeException(
                        "添付ファイルが存在しません。 storageType="
                                + attachment.getStorageType()
                );
            }
        }
    }
}
