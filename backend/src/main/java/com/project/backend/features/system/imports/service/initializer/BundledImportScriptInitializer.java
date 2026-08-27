package com.project.backend.features.system.imports.service.initializer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;

import lombok.RequiredArgsConstructor;

/**
 * 同梱した取込スクリプトをLOCALまたはS3へ初期登録する。
 * 管理画面で更新済みのファイルを保護するため、既存ファイルは上書きしない。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.imports.init",
        name = "enabled",
        havingValue = "true"
)
public class BundledImportScriptInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(
            BundledImportScriptInitializer.class
    );
    private static final List<String> SCRIPT_FILE_NAMES = List.of(
            "convert_care_insurance_rate.py",
            "convert_child_care_support_fund.py",
            "convert_employment_insurance_rate.py",
            "convert_health_insurance_rate.py",
            "convert_income_tax_table.py",
            "convert_income_tax_table_v2.py",
            "convert_pension_insurance_rate.py",
            "convert_resident_tax.py",
            "convert_standard_monthly_remuneration.py"
    );

    private final StorageService storageService;
    private final StorageProperties storageProperties;

    @Override
    public void run(ApplicationArguments args) {
        SCRIPT_FILE_NAMES.forEach(this::initializeIfMissing);
    }

    private void initializeIfMissing(String fileName) {
        String prefix = storageProperties.getImports()
                .getScript()
                .getPath()
                .replace("\\", "/")
                .replaceAll("^/+|/+$", "");
        String storageKey = prefix + "/" + fileName;

        if (storageService.exists(storageKey)) {
            log.info("Bundled import script already exists. key={}", storageKey);
            return;
        }

        ClassPathResource resource = new ClassPathResource(
                "imports/scripts/" + fileName
        );
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Bundled import script is missing. fileName=" + fileName
            );
        }

        try (var inputStream = resource.getInputStream()) {
            storageService.save(
                    storageKey,
                    inputStream,
                    resource.contentLength(),
                    "text/x-python; charset=UTF-8"
            );
            log.info("Bundled import script initialized. key={}", storageKey);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Bundled import script initialization failed. key="
                            + storageKey,
                    exception
            );
        }
    }
}
