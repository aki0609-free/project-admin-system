package com.project.backend.features.system.report.service.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.properties.StorageProperties;

class ReportHtmlTemplateKeyBuilderTest {

    private final ReportHtmlTemplateKeyBuilder builder =
            new ReportHtmlTemplateKeyBuilder(new StorageProperties());

    @Test
    void buildsVersionedDocumentTemplateKey() {
        assertThat(builder.build("DAILY_LABOR_COST_PREVIEW", 2))
                .isEqualTo(
                        "documents/templates/reports/html/"
                                + "DAILY_LABOR_COST_PREVIEW/v2/"
                                + "template.html"
                );
    }

    @Test
    void rejectsTraversalKey() {
        assertThatThrownBy(() -> builder.validateKey(
                "documents/templates/reports/html/../secret.html"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
