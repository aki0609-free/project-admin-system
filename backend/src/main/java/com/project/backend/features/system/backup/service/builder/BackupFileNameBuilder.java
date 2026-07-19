package com.project.backend.features.system.backup.service.builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BackupFileNameBuilder {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public String buildCsvFileName(String targetCode, String fileNamePattern) {
        String timestamp = LocalDateTime.now().format(FORMATTER);

        if (StringUtils.hasText(fileNamePattern)) {
            return fileNamePattern
                    .replace("{targetCode}", targetCode)
                    .replace("{timestamp}", timestamp);
        }

        return targetCode + "_" + timestamp + ".csv";
    }

    public String buildZipFileName() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        return "backup_" + timestamp + ".zip";
    }
}