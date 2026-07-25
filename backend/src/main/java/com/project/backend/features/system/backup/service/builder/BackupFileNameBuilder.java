package com.project.backend.features.system.backup.service.builder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BackupFileNameBuilder {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final Clock clock;

    public String buildCsvFileName(String targetCode, String fileNamePattern) {
        String timestamp = uniqueTimestamp();

        if (StringUtils.hasText(fileNamePattern)) {
            return fileNamePattern
                    .replace("{targetCode}", targetCode)
                    .replace("{timestamp}", timestamp);
        }

        return targetCode + "_" + timestamp + ".csv";
    }

    public String buildZipFileName() {
        String timestamp = uniqueTimestamp();
        return "backup_" + timestamp + ".zip";
    }

    private String uniqueTimestamp() {
        return LocalDateTime.now(clock).format(FORMATTER)
                + "_"
                + UUID.randomUUID().toString().substring(0, 8);
    }
}
