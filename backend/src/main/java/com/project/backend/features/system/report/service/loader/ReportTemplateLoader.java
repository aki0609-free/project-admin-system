package com.project.backend.features.system.report.service.loader;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.report.service.builder.ReportTemplateKeyBuilder;
import com.project.backend.features.system.report.service.validation.ReportTemplateValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportTemplateLoader {

    private final StorageService storageService;
    private final ReportTemplateValidator validator;
    private final ReportTemplateKeyBuilder keyBuilder;

    public String load(String templateFileName) {
        validator.validateFileName(templateFileName);

        String fileKey = keyBuilder.build(templateFileName);

        if (!storageService.exists(fileKey)) {
            throw new RuntimeException("帳票テンプレートファイルが存在しません。 fileKey=" + fileKey);
        }

        try (var inputStream = storageService.load(fileKey)) {
            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            throw new RuntimeException("テンプレート読み込みに失敗しました。 fileKey=" + fileKey, e);
        }
    }
}