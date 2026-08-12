package com.project.backend.features.system.report.service.initializer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.system.report.service.builder.ReportTemplateKeyBuilder;

class BundledReportTemplateInitializerTest {

    private static final List<String> TEMPLATE_FILES = List.of(
            "monthly_pay_slip.jrxml",
            "daily_pay_slip.jrxml",
            "monthly_invoice_pattern_1.jrxml",
            "monthly_invoice_pattern_2.jrxml",
            "monthly_invoice_pattern_3.jrxml",
            "monthly_order_form.jrxml",
            "daily_work_order.jrxml",
            "monthly_labor_cost_list.xlsx"
    );

    private final StorageService storageService =
            org.mockito.Mockito.mock(StorageService.class);

    private final ReportTemplateKeyBuilder keyBuilder =
            org.mockito.Mockito.mock(ReportTemplateKeyBuilder.class);

    private final BundledReportTemplateInitializer initializer =
            new BundledReportTemplateInitializer(
                    storageService,
                    keyBuilder
            );

    @Test
    void savesBundledTemplateWhenStorageFileDoesNotExist() {
        stubTemplatePaths(false);

        initializer.run(new DefaultApplicationArguments());

        for (String fileName : TEMPLATE_FILES) {
            String contentType = fileName.endsWith(".xlsx")
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "application/xml";
            verify(storageService).save(
                    eq("reports/" + fileName),
                    isA(InputStream.class),
                    longThat(size -> size > 0),
                    eq(contentType)
            );
        }
    }

    @Test
    void preservesExistingStorageTemplate() {
        stubTemplatePaths(true);

        initializer.run(new DefaultApplicationArguments());

        for (String fileName : TEMPLATE_FILES) {
            String contentType = fileName.endsWith(".xlsx")
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "application/xml";
            verify(storageService, never()).save(
                    eq("reports/" + fileName),
                    isA(InputStream.class),
                    org.mockito.ArgumentMatchers.anyLong(),
                    eq(contentType)
            );
        }
    }

    private void stubTemplatePaths(boolean exists) {
        for (String fileName : TEMPLATE_FILES) {
            String key = "reports/" + fileName;
            when(keyBuilder.build(fileName)).thenReturn(key);
            when(storageService.exists(key)).thenReturn(exists);
        }
    }
}
