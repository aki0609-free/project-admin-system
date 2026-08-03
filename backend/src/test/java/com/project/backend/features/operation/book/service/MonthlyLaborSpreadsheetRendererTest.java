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
                        Map.entry("holiday_pay_amount", new BigDecimal("1350")),
                        Map.entry("night_pay_amount", BigDecimal.ZERO),
                        Map.entry("vehicle_allowance_amount", new BigDecimal("500")),
                        Map.entry("other_allowance_amount", BigDecimal.ZERO),
                        Map.entry("gross_pay_amount", new BigDecimal("11500")),
                        Map.entry("tax_other_amount", new BigDecimal("500")),
                        Map.entry("saving_amount", new BigDecimal("1000")),
                        Map.entry("loan_repayment_amount", BigDecimal.ZERO),
                        Map.entry("advance_payment_amount", new BigDecimal("5000")),
                        Map.entry("other_deduction_amount", BigDecimal.ZERO),
                        Map.entry("available_payment_amount", new BigDecimal("10000")),
                        Map.entry("dormitory_fee_amount", new BigDecimal("1700")),
                        Map.entry("other_daily_amount", BigDecimal.ZERO),
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
        assertThat(sheet.path("rows").get(3)
                .path("cells").get(6).path("value").asDouble())
                .isEqualTo(10000d);
        assertThat(sheet.path("rows").get(3)
                .path("cells").get(8).path("value").asDouble())
                .isEqualTo(1350d);
        assertThat(sheet.path("rows").get(3)
                .path("cells").get(19).path("value").asDouble())
                .isEqualTo(1700d);
        assertThat(result.path("projectAdminMetadata")
                .path("fitToOnePage").asBoolean()).isTrue();
    }
}
