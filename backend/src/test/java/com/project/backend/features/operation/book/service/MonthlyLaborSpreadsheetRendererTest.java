package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;

class MonthlyLaborSpreadsheetRendererTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MonthlyLaborSpreadsheetRenderer renderer =
            new MonthlyLaborSpreadsheetRenderer(objectMapper);

    @Test
    void rendersOneEmployeeAsOneSheetWithThirtyOneDailyRows() {
        ExcelBookMaster master = new ExcelBookMaster();
        master.setBookCode("MONTHLY_LABOR");

        var result = renderer.render(new SpreadsheetLedgerRenderContext(
                objectMapper.createObjectNode(),
                master,
                List.of(Map.ofEntries(
                        Map.entry("employee_code", "E001"),
                        Map.entry("employee_name", "山田 太郎"),
                        Map.entry("company_name", "株式会社富陽"),
                        Map.entry("work_date", LocalDate.of(2026, 7, 1)),
                        Map.entry("customer_name", "元請A"),
                        Map.entry("site_name", "現場A"),
                        Map.entry("work_time", "08:00～17:00"),
                        Map.entry("total_hours", new BigDecimal("8.00")),
                        Map.entry("normal_pay_amount", new BigDecimal("10000")),
                        Map.entry("overtime_pay_amount", new BigDecimal("1000")),
                        Map.entry("night_pay_amount", new BigDecimal("250")),
                        Map.entry("driver_allowance_amount", new BigDecimal("500")),
                        Map.entry("attendance_attitude_allowance_amount", new BigDecimal("300")),
                        Map.entry("management_allowance_amount", new BigDecimal("400")),
                        Map.entry("other_allowance_amount", new BigDecimal("200")),
                        Map.entry("gross_pay_amount", new BigDecimal("12650")),
                        Map.entry("legal_deposit_amount", new BigDecimal("500")),
                        Map.entry("saving_amount", new BigDecimal("1000")),
                        Map.entry("saving_withdrawal_amount", new BigDecimal("800")),
                        Map.entry("short_term_advance_amount", new BigDecimal("600")),
                        Map.entry("loan_repayment_amount", new BigDecimal("700")),
                        Map.entry("dormitory_fee_amount", new BigDecimal("1700")),
                        Map.entry("mobile_rental_amount", new BigDecimal("900")),
                        Map.entry("wifi_fee_amount", new BigDecimal("1000")),
                        Map.entry("other_deduction_amount", new BigDecimal("400")),
                        Map.entry("note", "確認済み")
                )),
                "2026-07",
                Instant.parse("2026-07-31T00:00:00Z"),
                Map.of("selectionValue", "1")
        ));

        var sheet = result.path("Workbook").path("sheets").get(0);
        assertThat(sheet.path("name").asText())
                .isEqualTo("2026-07 山田 太郎");
        assertThat(sheet.path("rows")).hasSize(35);
        assertThat(sheet.path("rows").get(0)
                .path("cells").get(0).path("value").asText())
                .isEqualTo(
                        "2026年07月分　No.E001　氏名：山田 太郎　　月間労務表　　株式会社富陽"
                );
        assertThat(sheet.path("rows").get(0)
                .path("cells").get(0).has("colSpan")).isFalse();
        assertThat(sheet.path("rows").get(0)
                .path("cells").get(0).path("style").path("textAlign").asText())
                .isEqualTo("Left");
        assertThat(sheet.path("rows").get(0)
                .path("cells").get(0).path("style").path("overflow").asText())
                .isEqualTo("visible");
        assertThat(sheet.path("rows").get(0).path("cells")).hasSize(24);
        assertThat(sheet.path("rows").get(0)
                .path("cells").get(23).path("style").path("backgroundColor").asText())
                .isEqualTo("#D9EAF7");
        assertThat(sheet.path("rows").get(0)
                .path("cells").get(0).path("style").path("borderRight").asText())
                .isEqualTo("none");
        assertThat(sheet.path("rows").get(0)
                .path("cells").get(1).path("style").path("borderLeft").asText())
                .isEqualTo("none");
        assertThat(sheet.path("rows").get(0)
                .path("cells").get(23).path("style").path("borderRight").asText())
                .isEqualTo("1px solid #777777");
        assertThat(sheet.path("rows").get(1)
                .path("cells").get(0).has("colSpan")).isFalse();
        assertThat(sheet.path("rows").get(1)
                .path("cells").get(0).path("style").path("overflow").asText())
                .isEqualTo("visible");
        assertThat(sheet.path("rows").get(1).path("cells")).hasSize(24);
        assertThat(sheet.path("rows").get(1)
                .path("cells").get(23).path("style").path("backgroundColor").asText())
                .isEqualTo("#F4F7FA");
        assertThat(sheet.path("rows").get(1)
                .path("cells").get(10).path("style").path("borderLeft").asText())
                .isEqualTo("none");
        assertThat(sheet.path("rows").get(1)
                .path("cells").get(10).path("style").path("borderRight").asText())
                .isEqualTo("none");
        assertThat(sheet.path("rows").get(3)
                .path("cells").get(6).path("value").asDouble())
                .isEqualTo(10000d);
        assertThat(sheet.path("rows").get(3)
                .path("cells").get(8).path("value").asDouble())
                .isEqualTo(250d);
        assertThat(sheet.path("rows").get(3)
                .path("cells").get(19).path("value").asDouble())
                .isEqualTo(1700d);
        assertThat(sheet.path("rows").get(2)
                .path("cells").get(9).path("value").asText())
                .isEqualTo("運転手当");
        assertThat(sheet.path("rows").get(2)
                .path("cells").get(10).path("style")
                .path("whiteSpace").asText())
                .isEqualTo("nowrap");
        assertThat(sheet.path("columns").get(10)
                .path("width").asDouble())
                .isEqualTo(92d);
        assertThat(sheet.path("columns").get(20)
                .path("width").asDouble())
                .isEqualTo(100d);
        assertThat(sheet.path("rows").get(2)
                .path("cells").get(17).path("value").asText())
                .isEqualTo("前借り");
        assertThat(sheet.path("rows").get(2).toString())
                .doesNotContain("休日", "当日支給額 B");
        assertThat(sheet.path("rows").get(3)
                .path("cells").get(16).path("value").asDouble())
                .isEqualTo(800d);
        assertThat(sheet.path("rows").get(3)
                .path("cells").get(20).path("value").asDouble())
                .isEqualTo(900d);
        assertThat(result.path("projectAdminMetadata")
                .path("fitToOnePage").asBoolean()).isTrue();
        assertThat(result.path("projectAdminMetadata")
                .path("governmentSubmissionPrintEndColumn").asText())
                .isEqualTo("N");
    }
}
