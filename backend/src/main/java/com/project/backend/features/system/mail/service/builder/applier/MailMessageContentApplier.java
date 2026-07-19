package com.project.backend.features.system.mail.service.builder.applier;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.project.backend.features.system.mail.entity.MailSendQueue;

@Component
public class MailMessageContentApplier {

    @SuppressWarnings("null")
    public void apply(MimeMessageHelper helper, MailSendQueue mail) throws Exception {
        helper.setSubject(mail.getSubject());
        helper.setText(mail.getBody(), mail.isHtmlFlag());
    }
}