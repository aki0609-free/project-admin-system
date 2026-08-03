package com.project.backend.features.system.report.service.loader;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.system.report.service.builder.ReportHtmlTemplateKeyBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportHtmlTemplateLoader {

    private static final String DEFAULT_PREVIEW_TEMPLATE =
            "templates/operation/reportpreview/default-table.html";

    private final StorageService storageService;
    private final ReportHtmlTemplateKeyBuilder keyBuilder;

    public String load(OperationReportPreview definition) {
        String key = resolveKey(definition);

        keyBuilder.validateKey(key);
        if (!storageService.exists(key)) {
            throw new IllegalStateException(
                    "HTML帳票テンプレートが存在しません。 key=" + key
            );
        }

        try (var inputStream = storageService.load(key)) {
            byte[] data = inputStream.readAllBytes();
            validateHash(data, definition.getHtmlTemplateHash());
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (e instanceof IllegalStateException stateException) {
                throw stateException;
            }
            throw new IllegalStateException(
                    "HTML帳票テンプレートの読み込みに失敗しました。 key="
                            + key,
                    e
            );
        }
    }

    public String loadOrDefault(OperationReportPreview definition) {
        String key = resolveKey(definition);
        keyBuilder.validateKey(key);

        if (storageService.exists(key)) {
            return load(definition);
        }

        ClassPathResource resource = new ClassPathResource(
                DEFAULT_PREVIEW_TEMPLATE
        );
        try (var inputStream = resource.getInputStream()) {
            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "共通帳票プレビューテンプレートの読み込みに失敗しました。",
                    e
            );
        }
    }

    private String resolveKey(OperationReportPreview definition) {
        return StringUtils.hasText(definition.getHtmlTemplateKey())
                ? definition.getHtmlTemplateKey()
                : keyBuilder.build(
                        definition.getReportCode(),
                        definition.getHtmlTemplateVersion()
                );
    }

    private void validateHash(byte[] data, String expectedHash) {
        if (!StringUtils.hasText(expectedHash)) {
            return;
        }

        try {
            String actualHash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data)
            );
            if (!actualHash.equalsIgnoreCase(expectedHash)) {
                throw new IllegalStateException(
                        "HTML帳票テンプレートのハッシュが一致しません。"
                );
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256を利用できません。",
                    e
            );
        }
    }
}
