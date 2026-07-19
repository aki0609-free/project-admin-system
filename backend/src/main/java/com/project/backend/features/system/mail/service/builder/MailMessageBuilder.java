package com.project.backend.features.system.mail.service.builder;

import java.nio.charset.StandardCharsets;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.project.backend.features.system.mail.entity.MailSendQueue;
import com.project.backend.features.system.mail.service.builder.applier.MailMessageAddressApplier;
import com.project.backend.features.system.mail.service.builder.applier.MailMessageAttachmentApplier;
import com.project.backend.features.system.mail.service.builder.applier.MailMessageContentApplier;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailMessageBuilder {

    private final JavaMailSender mailSender;
    private final MailMessageAddressApplier addressApplier;
    private final MailMessageContentApplier contentApplier;
    private final MailMessageAttachmentApplier attachmentApplier;

    public MimeMessage build(MailSendQueue mail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    hasAttachments(mail),
                    StandardCharsets.UTF_8.name()
            );

            addressApplier.apply(helper, mail);
            contentApplier.apply(helper, mail);
            attachmentApplier.apply(helper, mail);

            return message;

        } catch (Exception e) {
            throw new RuntimeException("メールメッセージ生成に失敗しました。", e);
        }
    }

    private boolean hasAttachments(MailSendQueue mail) {
        return mail.getAttachments() != null
                && !mail.getAttachments().isEmpty();
    }
}