package com.project.backend.features.system.report.service.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.report.entity.ReportMaster;

class ReportFileNameBuilderTest {

    private final ReportFileNameBuilder builder =
            new ReportFileNameBuilder(Clock.fixed(
                    Instant.parse("2026-12-31T15:00:01Z"),
                    ZoneId.of("Asia/Tokyo")
            ));

    @Test
    void build_shouldUseTokyoDateAcrossYearBoundary() {
        ReportMaster master = new ReportMaster();
        master.setReportCode("MONTHLY_PAY_SLIP");
        master.setFileName("salary_slip");

        assertThat(builder.build(master, "pdf"))
                .isEqualTo(
                        "salary_slip_20270101000001.pdf"
                );
    }

    @Test
    void build_shouldFallbackToReportCode() {
        ReportMaster master = new ReportMaster();
        master.setReportCode("MONTHLY_INVOICE");

        assertThat(builder.build(master, "xlsx"))
                .isEqualTo(
                        "MONTHLY_INVOICE_20270101000001.xlsx"
                );
    }
}
