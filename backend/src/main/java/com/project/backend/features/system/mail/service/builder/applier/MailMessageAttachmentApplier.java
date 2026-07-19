package com.project.backend.features.system.mail.service.builder.applier;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.dto.MailAttachmentResource;
import com.project.backend.features.system.mail.entity.MailSendAttachment;
import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.service.loader.MailAttachmentLoader;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailMessageAttachmentApplier {

    private final MailAttachmentLoader attachmentLoader;

    @SuppressWarnings("null")
    public void apply(MimeMessageHelper helper, MailSendQueue mail) throws Exception {
        if (mail.getAttachments() == null || mail.getAttachments().isEmpty()) {
            return;
        }

        for (MailSendAttachment attachment : mail.getAttachments()) {
            MailAttachmentResource resource = attachmentLoader.load(attachment);

            if (StringUtils.hasText(resource.mimeType())) {
                helper.addAttachment(
                        resource.fileName(),
                        resource.source(),
                        resource.mimeType()
                );
            } else {
                helper.addAttachment(
                        resource.fileName(),
                        resource.source()
                );
            }
        }
    }
}
