package com.project.backend.features.system.report.service.resolver;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.report.config.ReportTemplateProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReportTemplatePathResolver {

    private final ReportTemplateProperties properties;

    public Path resolve(
            String fileName
    ) {

        Path basePath =
                Path.of(
                        properties.getTemplateBaseDir()
                ).normalize();

        Path templatePath =
                basePath.resolve(fileName)
                        .normalize();

        if (!templatePath.startsWith(basePath)) {
            throw new RuntimeException(
                    "不正なテンプレートパスです。"
            );
        }

        return templatePath;
    }
}