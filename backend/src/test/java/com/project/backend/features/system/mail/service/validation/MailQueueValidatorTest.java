package com.project.backend.features.system.mail.service.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.mail.entity.MailSendAttachment;
import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.enums.MailStorageType;
import com.project.backend.features.system.mail.service.loader.MailAttachmentLoader;

class MailQueueValidatorTest {

    @Test
    void validateBeforeSend_shouldCheckAttachmentUsingRecordedStorageType() {
        MailAttachmentLoader attachmentLoader = mock(MailAttachmentLoader.class);
        MailQueueValidator validator = new MailQueueValidator(attachmentLoader);
        MailSendQueue mail = validMail();
        MailSendAttachment attachment = attachment(MailStorageType.S3);
        mail.addAttachment(attachment);
        when(attachmentLoader.exists(attachment)).thenReturn(true);

        validator.validateBeforeSend(mail);

        verify(attachmentLoader).exists(attachment);
    }

    @Test
    void validateBeforeSend_shouldRejectMissingAttachmentStorageType() {
        MailAttachmentLoader attachmentLoader = mock(MailAttachmentLoader.class);
        MailQueueValidator validator = new MailQueueValidator(attachmentLoader);
        MailSendQueue mail = validMail();
        mail.addAttachment(attachment(null));

        assertThatThrownBy(() -> validator.validateBeforeSend(mail))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("storageType");
        verifyNoInteractions(attachmentLoader);
    }

    private MailSendQueue validMail() {
        MailSendQueue mail = new MailSendQueue();
        mail.setFromAddress("from@example.com");
        mail.setToAddress("to@example.com");
        mail.setSubject("subject");
        mail.setBody("body");
        return mail;
    }

    private MailSendAttachment attachment(MailStorageType storageType) {
        MailSendAttachment attachment = new MailSendAttachment();
        attachment.setStorageType(storageType);
        attachment.setFileKey("reports/file.pdf");
        attachment.setFileName("file.pdf");
        return attachment;
    }
}
