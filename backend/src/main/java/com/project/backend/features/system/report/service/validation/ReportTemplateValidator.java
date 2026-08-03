package com.project.backend.features.system.report.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReportTemplateValidator {

    public void validateFileName(String fileName) {
        validateJrxmlFileName(fileName);
    }

    public void validateJrxmlFileName(String fileName) {
        validateCommon(fileName);

        if (!fileName.toLowerCase().endsWith(".jrxml")) {
            throw new RuntimeException("テンプレートファイルは .jrxml を指定してください。");
        }
    }

    public void validateExcelFileName(String fileName) {
        validateCommon(fileName);

        if (!fileName.toLowerCase().endsWith(".xlsx")) {
            throw new RuntimeException("Excelテンプレートファイルは .xlsx を指定してください。");
        }
    }

    private void validateCommon(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new RuntimeException("テンプレートファイル名は必須です。");
        }

        if (fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("..")) {
            throw new RuntimeException("不正なテンプレートファイル名です。");
        }
    }
}
