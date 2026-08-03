package com.project.backend.features.operation.book.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.system.excelbook.entity.ExcelBookMaster;

class LaborCostPaymentSpreadsheetRendererTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LaborCostPaymentSpreadsheetRenderer renderer =
            new LaborCostPaymentSpreadsheetRenderer(objectMapper);

    @Test
    void render_shouldCreatePaymentCycleSheetsAndMonthlyTotals() {
        ExcelBookMaster master = master();
        List<Map<String, Object>> rows = List.of(
                row(1, "E001", "山田 太郎", "DAILY", 1,
                        "10000", "500", "1000", "-3000", "200"),
                row(1, "E001", "山田 太郎", "DAILY", 2,
                        "11000", "500", "1000", "-3000", "200"),
                row(2, "E002", "鈴木 花子", "MONTHLY", 1,
                        "16000", "0", "0", "0", "0")
        );

        var result = renderer.render(context(master, rows));
        var sheets = result.path("Workbook").path("sheets");

        assertThat(sheets).hasSize(2);
        assertThat(sheets.get(0).path("name").asText()).isEqualTo("日払い");
        assertThat(sheets.get(1).path("name").asText()).isEqualTo("月払い");

        var dailyRows = sheets.get(0).path("rows");
        assertThat(dailyRows).hasSize(45);
        assertThat(dailyRows.get(5).path("cells").get(2)
                .path("value").asDouble()).isEqualTo(10000d);
        assertThat(dailyRows.get(36).path("cells").get(1)
                .path("value").asDouble()).isEqualTo(21000d);
        assertThat(dailyRows.get(42).path("cells").get(1)
                .path("value").asDouble()).isEqualTo(400d);
        assertThat(dailyRows.get(44).path("cells").get(1)
                .path("value").asDouble()).isEqualTo(20600d);
        assertThat(result.path("projectAdminMetadata")
                .path("paperSize").asText()).isEqualTo("A4");
    }

    @Test
    void render_shouldSplitEveryTenEmployees() {
        ExcelBookMaster master = master();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 1; index <= 11; index++) {
            rows.add(row(
                    index,
                    "E%03d".formatted(index),
                    "従業員" + index,
                    "DAILY",
                    1,
                    "10000",
                    "0",
                    "0",
                    "0",
                    "0"
            ));
        }

        var sheets = renderer.render(context(master, rows))
                .path("Workbook").path("sheets");

        assertThat(sheets).hasSize(2);
        assertThat(sheets.get(0).path("name").asText()).isEqualTo("日払い");
        assertThat(sheets.get(1).path("name").asText()).isEqualTo("日払い 2");
        assertThat(sheets.get(0).path("usedRange").path("colIndex").asInt())
                .isEqualTo(12);
        assertThat(sheets.get(1).path("usedRange").path("colIndex").asInt())
                .isEqualTo(3);
    }

    private SpreadsheetLedgerRenderContext context(
            ExcelBookMaster master,
            List<Map<String, Object>> rows
    ) {
        return new SpreadsheetLedgerRenderContext(
                objectMapper.createObjectNode(),
                master,
                rows,
                "2026-07",
                Instant.parse("2026-07-31T00:00:00Z"),
                Map.of()
        );
    }

    private ExcelBookMaster master() {
        ExcelBookMaster master = new ExcelBookMaster();
        master.setBookCode("LABOR_COST_PAYMENT");
        return master;
    }

    private Map<String, Object> row(
            long employeeId,
            String employeeCode,
            String employeeName,
            String paymentCycle,
            int day,
            String gross,
            String incomeTax,
            String savingRepayment,
            String borrowWithdrawal,
            String otherDeduction
    ) {
        return Map.ofEntries(
                Map.entry("employee_id", employeeId),
                Map.entry("employee_code", employeeCode),
                Map.entry("employee_name", employeeName),
                Map.entry("payment_cycle", paymentCycle),
                Map.entry("company_name", "株式会社富陽"),
                Map.entry("work_date", LocalDate.of(2026, 7, day)),
                Map.entry("daily_gross_amount", new BigDecimal(gross)),
                Map.entry("income_tax_amount", new BigDecimal(incomeTax)),
                Map.entry("saving_repayment_amount", new BigDecimal(savingRepayment)),
                Map.entry("borrow_withdrawal_amount", new BigDecimal(borrowWithdrawal)),
                Map.entry("other_deduction_amount", new BigDecimal(otherDeduction))
        );
    }
}
