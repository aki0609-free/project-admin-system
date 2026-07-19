package com.project.backend.features.system.report.service.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.project.backend.app.storage.properties.StorageProperties;
import com.project.backend.features.system.report.entity.ReportMaster;

class ReportFileKeyBuilderTest {

    @Test
    void buildRecipient_shouldUseOpaqueGroupId() {
        StorageProperties properties = new StorageProperties();
        properties.getOutput().setPath("reports-output");
        ReportFileKeyBuilder builder = new ReportFileKeyBuilder(properties);

        ReportMaster reportMaster = new ReportMaster();
        reportMaster.setReportCode("MONTHLY_PAY_SLIP");

        String key = builder.buildRecipient(
                reportMaster,
                "execution-1",
                "MONTHLY_PAY_SLIP:2026-07:employee-1",
                "給与明細.pdf"
        );

        assertThat(key)
                .startsWith("reports-output/MONTHLY_PAY_SLIP/execution-1/recipients/")
                .endsWith("/給与明細.pdf")
                .doesNotContain("employee-1")
                .doesNotContain("2026-07");
    }

    @Test
    void buildRecipient_shouldCreateDifferentKeysForDifferentBusinessKeys() {
        StorageProperties properties = new StorageProperties();
        ReportFileKeyBuilder builder = new ReportFileKeyBuilder(properties);
        ReportMaster reportMaster = new ReportMaster();
        reportMaster.setReportCode("REPORT");

        String first = builder.buildRecipient(
                reportMaster,
                "execution-1",
                "business-1",
                "report.pdf"
        );
        String second = builder.buildRecipient(
                reportMaster,
                "execution-1",
                "business-2",
                "report.pdf"
        );

        assertThat(first).isNotEqualTo(second);
    }
}
