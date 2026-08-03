package com.project.backend.features.system.report.service.builder;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.project.backend.app.storage.properties.StorageProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportHtmlTemplateKeyBuilder {

    private static final Pattern SAFE_REPORT_CODE =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,99}$");

    private final StorageProperties storageProperties;

    public String build(String reportCode, Integer version) {
        if (reportCode == null
                || !SAFE_REPORT_CODE.matcher(reportCode).matches()) {
            throw new IllegalArgumentException(
                    "安全なreportCodeを指定してください。"
            );
        }
        int resolvedVersion = version != null ? version : 1;
        if (resolvedVersion < 1) {
            throw new IllegalArgumentException(
                    "HTMLテンプレートVersionは1以上で指定してください。"
            );
        }

        return templatePrefix()
                + "/" + reportCode
                + "/v" + resolvedVersion
                + "/template.html";
    }

    public String templatePrefix() {
        StorageProperties.Document document =
                storageProperties.getDocument();

        return trimSlashes(document.getRootPath())
                + "/" + trimSlashes(document.getTemplatesPath())
                + "/reports/html";
    }

    public void validateKey(String key) {
        if (key == null
                || key.isBlank()
                || key.startsWith("/")
                || key.contains("..")
                || key.contains("\\")
                || !key.endsWith(".html")
                || !key.startsWith(templatePrefix() + "/")) {
            throw new IllegalArgumentException(
                    "HTMLテンプレートキーが許可範囲外です。"
            );
        }
    }

    private String trimSlashes(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "書類ストレージのパス設定が不足しています。"
            );
        }

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
