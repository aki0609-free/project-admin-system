package com.project.backend.features.system.mail.service.builder.applier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.project.backend.features.system.mail.dto.MailAttachmentResource;
import com.project.backend.features.system.mail.entity.MailSendAttachment;
import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.enums.MailStorageType;
import com.project.backend.features.system.mail.service.loader.MailAttachmentLoader;

class MailMessageAttachmentApplierTest {

    @Test
    void apply_shouldUseAttachmentLoaderResource() throws Exception {
        MailAttachmentLoader attachmentLoader = mock(MailAttachmentLoader.class);
        MailMessageAttachmentApplier applier = new MailMessageAttachmentApplier(
                attachmentLoader
        );
        MimeMessageHelper helper = mock(MimeMessageHelper.class);
        MailSendAttachment attachment = new MailSendAttachment();
        attachment.setStorageType(MailStorageType.S3);
        attachment.setFileKey("reports/file.pdf");
        attachment.setFileName("file.pdf");
        MailSendQueue mail = new MailSendQueue();
        mail.addAttachment(attachment);
        ByteArrayResource source = new ByteArrayResource(new byte[]{1, 2, 3});
        MailAttachmentResource resource = new MailAttachmentResource(
                "file.pdf",
                "application/pdf",
                source
        );
        when(attachmentLoader.load(attachment)).thenReturn(resource);

        applier.apply(helper, mail);

        verify(attachmentLoader).load(attachment);
        verify(helper).addAttachment(
                eq("file.pdf"),
                same(source),
                eq("application/pdf")
        );
    }
}
