package com.project.backend.features.system.report.service.initializer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.report.service.builder.ReportHtmlTemplateKeyBuilder;

class BundledReportHtmlTemplateInitializerTest {

    private static final Map<String, String> TEMPLATE_KEYS = Map.of(
            "DAILY_LABOR_COST_PREVIEW",
            "documents/templates/reports/html/"
                    + "DAILY_LABOR_COST_PREVIEW/v1/template.html",
            "DAILY_PAYMENT_PREPARATION",
            "documents/templates/reports/html/"
                    + "DAILY_PAYMENT_PREPARATION/v1/template.html"
    );

    private final StorageService storageService =
            Mockito.mock(StorageService.class);
    private final ReportHtmlTemplateKeyBuilder keyBuilder =
            Mockito.mock(ReportHtmlTemplateKeyBuilder.class);
    private final BundledReportHtmlTemplateInitializer initializer =
            new BundledReportHtmlTemplateInitializer(
                    storageService,
                    keyBuilder
            );

    @Test
    void savesMissingHtmlTemplates() {
        stubKeys(false);

        initializer.run(new DefaultApplicationArguments());

        for (String key : TEMPLATE_KEYS.values()) {
            verify(storageService).save(
                    eq(key),
                    isA(InputStream.class),
                    longThat(size -> size > 0),
                    eq("text/html; charset=UTF-8")
            );
        }
    }

    @Test
    void preservesExistingHtmlTemplates() {
        stubKeys(true);

        initializer.run(new DefaultApplicationArguments());

        for (String key : TEMPLATE_KEYS.values()) {
            verify(storageService, never()).save(
                    eq(key),
                    isA(InputStream.class),
                    org.mockito.ArgumentMatchers.anyLong(),
                    eq("text/html; charset=UTF-8")
            );
        }
    }

    private void stubKeys(boolean exists) {
        TEMPLATE_KEYS.forEach((reportCode, key) -> {
            when(keyBuilder.build(reportCode, 1)).thenReturn(key);
            when(storageService.exists(key)).thenReturn(exists);
        });
    }
}
