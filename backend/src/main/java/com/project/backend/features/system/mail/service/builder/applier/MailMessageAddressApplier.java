package com.project.backend.features.system.mail.service.builder.applier;

import java.util.Arrays;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.mail.entity.MailSendQueue;

@Component
public class MailMessageAddressApplier {

    @SuppressWarnings("null")
    public void apply(MimeMessageHelper helper, MailSendQueue mail) throws Exception {
        helper.setFrom(
                mail.getFromAddress(),
                StringUtils.hasText(mail.getFromName())
                        ? mail.getFromName()
                        : mail.getFromAddress()
        );

        helper.setTo(splitAddresses(mail.getToAddress()));

        if (StringUtils.hasText(mail.getCc())) {
            helper.setCc(splitAddresses(mail.getCc()));
        }

        if (StringUtils.hasText(mail.getBcc())) {
            helper.setBcc(splitAddresses(mail.getBcc()));
        }
    }

    private String[] splitAddresses(String value) {
        if (!StringUtils.hasText(value)) {
            return new String[0];
        }

        return Arrays.stream(value.split("\\s*,\\s*"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toArray(String[]::new);
    }
}