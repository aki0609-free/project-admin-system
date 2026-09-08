package com.project.backend.features.system.report.service.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    @Test
    void build_shouldResolveCamelCasePlaceholderFromSnakeCaseOutputColumn() {
        ReportMaster master = new ReportMaster();
        master.setReportCode("DAILY_WORK_ORDER");
        master.setFileName("作業証明伝票_${targetDate}");

        assertThat(builder.build(
                master,
                "pdf",
                List.of(Map.of("target_date", LocalDate.of(2026, 9, 5)))
        )).isEqualTo("作業証明伝票_2026-09-05_20270101000001.pdf");
    }

    @Test
    void build_shouldFormatMonthPlaceholderWithoutFirstDay() {
        ReportMaster master = new ReportMaster();
        master.setReportCode("MONTHLY_INVOICE");
        master.setFileName("請求書_${targetMonth}_${customerId}_v${closingVersion}");

        assertThat(builder.build(
                master,
                "pdf",
                List.of(Map.of(
                        "target_month", LocalDate.of(2026, 9, 1),
                        "customer_id", 12L,
                        "closing_version", 3
                ))
        )).isEqualTo("請求書_2026-09_12_v3_20270101000001.pdf");
    }
}
