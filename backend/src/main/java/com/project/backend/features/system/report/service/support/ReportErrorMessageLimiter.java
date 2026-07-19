package com.project.backend.features.system.report.service.support;

import org.springframework.stereotype.Component;

@Component
public class ReportErrorMessageLimiter {

    private static final int DEFAULT_MAX_LENGTH = 4000;

    public String limit(String value) {
        if (value == null) {
            return null;
        }

        return value.length() <= DEFAULT_MAX_LENGTH
                ? value
                : value.substring(0, DEFAULT_MAX_LENGTH);
    }
}