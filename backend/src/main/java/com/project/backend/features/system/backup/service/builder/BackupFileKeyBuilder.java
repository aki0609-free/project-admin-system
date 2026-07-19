package com.project.backend.features.system.backup.service.builder;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.properties.StorageProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BackupFileKeyBuilder {

    private final StorageProperties storageProperties;

    public String build(
            String outputDir,
            String fileName
    ) {
        return joinPath(
                storageProperties.getOutput().getPath(),
                outputDir,
                fileName
        );
    }

    private String joinPath(String... values) {
        StringBuilder builder = new StringBuilder();

        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }

            String normalized = value
                    .replace("\\", "/")
                    .replace("..", "_")
                    .trim();

            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }

            while (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }

            if (normalized.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append("/");
            }

            builder.append(normalized);
        }

        return builder.toString();
    }
}