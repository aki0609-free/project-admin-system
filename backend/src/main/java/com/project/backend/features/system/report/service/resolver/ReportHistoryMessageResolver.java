package com.project.backend.features.system.report.service.resolver;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReportHistoryMessageResolver {

    public String failureMessage(Exception exception) {
        if (exception == null) {
            return null;
        }

        String message = exception.getMessage();

        return StringUtils.hasText(message)
                ? message
                : exception.getClass().getSimpleName();
    }

    public String notes(String notes, String executionId) {
        String executionText = "executionId=" + executionId;

        if (!StringUtils.hasText(notes)) {
            return executionText;
        }

        return notes + " / " + executionText;
    }
}