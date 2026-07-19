package com.project.backend.features.system.report.service.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.properties.StorageProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportTemplateKeyBuilder {

    private final StorageProperties storageProperties;

    public String build(String templateFileName) {
        if (!StringUtils.hasText(templateFileName)) {
            throw new RuntimeException("templateFileName が未設定です。");
        }

        return templatePrefix() + "/" + templateFileName;
    }

    public String templatePrefix() {
        String path = storageProperties.getTemplate() != null
                ? storageProperties.getTemplate().getPath()
                : null;

        if (!StringUtils.hasText(path)) {
            return "templates/reports";
        }

        return trimSlashes(path);
    }

    private String trimSlashes(String value) {
        String result = value.trim();

        while (result.startsWith("/")) {
            result = result.substring(1);
        }

        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }
}