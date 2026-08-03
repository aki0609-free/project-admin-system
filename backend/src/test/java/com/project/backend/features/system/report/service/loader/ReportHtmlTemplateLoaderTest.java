package com.project.backend.features.system.report.service.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.app.storage.service.StorageService;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.system.report.service.builder.ReportHtmlTemplateKeyBuilder;

class ReportHtmlTemplateLoaderTest {

    private static final String TEMPLATE_KEY =
            "documents/templates/reports/html/"
                    + "DAILY_LABOR_COST_PREVIEW/v1/template.html";

    private final StorageService storageService =
            Mockito.mock(StorageService.class);
    private final ReportHtmlTemplateLoader loader =
            new ReportHtmlTemplateLoader(
                    storageService,
                    new ReportHtmlTemplateKeyBuilder(
                            new StorageProperties()
                    )
            );

    @Test
    void loadsTemplateFromConfiguredStorage() {
        OperationReportPreview definition = definition();
        byte[] content = "<html>preview</html>".getBytes(
                StandardCharsets.UTF_8
        );
        when(storageService.exists(TEMPLATE_KEY)).thenReturn(true);
        when(storageService.load(TEMPLATE_KEY)).thenReturn(
                new ByteArrayInputStream(content)
        );

        assertThat(loader.load(definition))
                .isEqualTo("<html>preview</html>");
    }

    @Test
    void rejectsUnexpectedTemplateHash() {
        OperationReportPreview definition = definition();
        definition.setHtmlTemplateHash("0".repeat(64));
        when(storageService.exists(TEMPLATE_KEY)).thenReturn(true);
        when(storageService.load(TEMPLATE_KEY)).thenReturn(
                new ByteArrayInputStream("changed".getBytes(
                        StandardCharsets.UTF_8
                ))
        );

        assertThatThrownBy(() -> loader.load(definition))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ハッシュ");
    }

    @Test
    void loadsCommonTableWhenOptionalPreviewTemplateDoesNotExist() {
        OperationReportPreview definition = definition();
        when(storageService.exists(TEMPLATE_KEY)).thenReturn(false);

        assertThat(loader.loadOrDefault(definition))
                .contains("<table")
                .contains("${columns}")
                .contains("${rows}");
    }

    private OperationReportPreview definition() {
        OperationReportPreview definition =
                new OperationReportPreview();
        definition.setReportCode("DAILY_LABOR_COST_PREVIEW");
        definition.setHtmlTemplateVersion(1);
        definition.setHtmlTemplateKey(TEMPLATE_KEY);
        return definition;
    }
}
