package com.project.backend.features.system.report.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReportTemplateValidator {

    public void validateFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new RuntimeException("テンプレートファイル名は必須です。");
        }

        if (fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("..")) {
            throw new RuntimeException("不正なテンプレートファイル名です。");
        }

        if (!fileName.endsWith(".jrxml")) {
            throw new RuntimeException("テンプレートファイルは .jrxml を指定してください。");
        }
    }
}