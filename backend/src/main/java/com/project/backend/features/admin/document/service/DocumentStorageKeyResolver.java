package com.project.backend.features.admin.document.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.features.admin.document.enums.DocumentArea;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DocumentStorageKeyResolver {

    private final StorageProperties properties;

    public String resolveAreaRoot(DocumentArea area) {
        if (area == null) {
            throw new IllegalArgumentException("documentArea は必須です。");
        }

        StorageProperties.Document document = properties.getDocument();

        return joinSegments(
                document.getRootPath(),
                switch (area) {
                    case GENERAL -> document.getGeneralPath();
                    case GENERATED_REPORTS -> document.getGeneratedReportsPath();
                    case BACKUPS -> document.getBackupsPath();
                    case TEMPLATES -> document.getTemplatesPath();
                }
        );
    }

    public String resolve(
            DocumentArea area,
            String relativePath
    ) {
        String areaRoot = resolveAreaRoot(area);

        if (!StringUtils.hasText(relativePath) || "/".equals(relativePath.trim())) {
            return areaRoot;
        }

        return areaRoot + "/" + normalizeRelativePath(relativePath);
    }

    private String normalizeRelativePath(String relativePath) {
        String value = relativePath.trim().replace("\\", "/");

        while (value.startsWith("/")) {
            value = value.substring(1);
        }

        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }

        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("relativePath が不正です。");
        }

        return joinSegments(value);
    }

    private String joinSegments(String... paths) {
        List<String> segments = new ArrayList<>();

        for (String path : paths) {
            if (!StringUtils.hasText(path)) {
                throw new IllegalStateException("書類ストレージのパス設定が未設定です。");
            }

            String normalized = path.trim().replace("\\", "/");

            for (String segment : normalized.split("/", -1)) {
                if (!StringUtils.hasText(segment)) {
                    continue;
                }

                validateSegment(segment);
                segments.add(segment);
            }
        }

        if (segments.isEmpty()) {
            throw new IllegalArgumentException("ストレージキーが空です。");
        }

        return String.join("/", segments);
    }

    private void validateSegment(String segment) {
        if (".".equals(segment) || "..".equals(segment)) {
            throw new IllegalArgumentException("相対パスは使用できません。");
        }

        if (segment.chars().anyMatch(character ->
                Character.isISOControl(character)
                        || character == '\u0000')) {
            throw new IllegalArgumentException("制御文字を含むパスは使用できません。");
        }
    }
}
