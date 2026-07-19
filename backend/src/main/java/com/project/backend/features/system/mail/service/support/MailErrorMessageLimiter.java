package com.project.backend.features.system.mail.service.support;

import org.springframework.stereotype.Component;

@Component
public class MailErrorMessageLimiter {

    private static final int MAX_LENGTH = 4000;

    public String limit(String value) {
        if (value == null) {
            return null;
        }

        return value.length() <= MAX_LENGTH
                ? value
                : value.substring(0, MAX_LENGTH);
    }
}