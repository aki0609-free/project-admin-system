package com.project.backend.features.system.excelbook.service.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.admin.document.enums.DocumentArea;
import com.project.backend.features.admin.document.service.DocumentStorageKeyResolver;

import lombok.RequiredArgsConstructor;

/**
 * リポジトリ同梱の初期SpreadsheetテンプレートをS3／LOCALへ登録する。
 * 保存済みテンプレートは上書きしない。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.spreadsheet.init",
        name = "enabled",
        havingValue = "true"
)
public class BundledSpreadsheetTemplateInitializer
        implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(
            BundledSpreadsheetTemplateInitializer.class
    );
    private static final java.util.List<BundledTemplate> TEMPLATES =
            java.util.List.of(
                    new BundledTemplate(
                            "spreadsheet/receipt_confirmation_template.json",
                            "ledgers/default/RECEIPT_CONFIRMATION/template.json"
                    ),
                    new BundledTemplate(
                            "spreadsheet/monthly_summary_template.json",
                            "ledgers/default/MONTHLY_SUMMARY/template.json"
                    )
            );

    private final StorageService storageService;
    private final DocumentStorageKeyResolver storageKeyResolver;

    @Override
    public void run(ApplicationArguments args) {
        TEMPLATES.forEach(this::initialize);
    }

    private void initialize(BundledTemplate template) {
        String storageKey = storageKeyResolver.resolve(
                DocumentArea.TEMPLATES,
                template.relativePath()
        );
        if (storageService.exists(storageKey)) {
            log.info(
                    "Bundled Spreadsheet template already exists. key={}",
                    storageKey
            );
            return;
        }

        ClassPathResource resource = new ClassPathResource(
                template.resourcePath()
        );
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Bundled Spreadsheet template is missing: "
                            + template.resourcePath()
            );
        }
        try (var inputStream = resource.getInputStream()) {
            storageService.save(
                    storageKey,
                    inputStream,
                    resource.contentLength(),
                    "application/json"
            );
            log.info(
                    "Bundled Spreadsheet template initialized. key={}",
                    storageKey
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Bundled Spreadsheet template initialization failed. key="
                            + storageKey,
                    exception
            );
        }
    }

    private record BundledTemplate(
            String resourcePath,
            String relativePath
    ) {
    }
}
