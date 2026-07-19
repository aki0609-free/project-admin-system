package com.project.backend.features.system.mail.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class MailTemplateRenderService {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    public String render(String template, Map<String, Object> values) {
        if (template == null) {
            return "";
        }

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = values != null ? values.get(key) : null;

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(value != null ? String.valueOf(value) : "")
            );
        }

        matcher.appendTail(result);
        return result.toString();
    }
}