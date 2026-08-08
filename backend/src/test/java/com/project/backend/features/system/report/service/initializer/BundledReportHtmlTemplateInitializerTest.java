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

    private static final Map<String, TemplateKey> TEMPLATE_KEYS = Map.of(
            "DAILY_LABOR_COST_PREVIEW",
            new TemplateKey(1, "documents/templates/reports/html/"
                    + "DAILY_LABOR_COST_PREVIEW/v1/template.html"),
            "DAILY_PAYMENT_PREPARATION",
            new TemplateKey(1, "documents/templates/reports/html/"
                    + "DAILY_PAYMENT_PREPARATION/v1/template.html"),
            "DAILY_PAY_SLIP",
            new TemplateKey(2, "documents/templates/reports/html/"
                    + "DAILY_PAY_SLIP/v2/template.html"),
            "MONTHLY_PAY_SLIP",
            new TemplateKey(1, "documents/templates/reports/html/"
                    + "MONTHLY_PAY_SLIP/v1/template.html")
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

        for (TemplateKey template : TEMPLATE_KEYS.values()) {
            verify(storageService).save(
                    eq(template.key()),
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

        for (TemplateKey template : TEMPLATE_KEYS.values()) {
            verify(storageService, never()).save(
                    eq(template.key()),
                    isA(InputStream.class),
                    org.mockito.ArgumentMatchers.anyLong(),
                    eq("text/html; charset=UTF-8")
            );
        }
    }

    private void stubKeys(boolean exists) {
        TEMPLATE_KEYS.forEach((reportCode, template) -> {
            when(keyBuilder.build(reportCode, template.version()))
                    .thenReturn(template.key());
            when(storageService.exists(template.key())).thenReturn(exists);
        });
    }

    private record TemplateKey(int version, String key) {
    }
}
