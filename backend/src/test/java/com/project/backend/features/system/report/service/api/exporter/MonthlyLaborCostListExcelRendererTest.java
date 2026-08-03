package com.project.backend.features.system.report.service.api.exporter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.project.backend.features.system.report.entity.ReportMaster;

class MonthlyLaborCostListExcelRendererTest {

    private final MonthlyLaborCostListExcelRenderer renderer =
            new MonthlyLaborCostListExcelRenderer();

    @Test
    void writesFixedValuesAndAddsRowsBeforeTheTotalRow() throws Exception {
        byte[] template = Files.readAllBytes(Path.of(
                "src/main/resources/reports/monthly_labor_cost_list.xlsx"
        ));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 1; index <= 35; index++) {
            rows.add(row(index));
        }

        ReportMaster master = new ReportMaster();
        master.setReportCode("MONTHLY_LABOR_COST_LIST");
        byte[] rendered = renderer.render(master, template, rows);

        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(rendered))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("2026.7");
            assertThat(sheet.getRow(0).getCell(5).getStringCellValue())
                    .isEqualTo("2026年");
            assertThat(sheet.getRow(0).getCell(6).getStringCellValue())
                    .isEqualTo("7月分");
            assertThat(sheet.getRow(0).getCell(7).getStringCellValue())
                    .isEqualTo("8/15支払");
            assertThat(sheet.getRow(0).getCell(26).getStringCellValue())
                    .isEqualTo("テスト株式会社");

            assertThat(sheet.getRow(3).getCell(0).getStringCellValue())
                    .isEqualTo("従業員1");
            assertThat(sheet.getRow(37).getCell(0).getStringCellValue())
                    .isEqualTo("従業員35");
            assertThat(sheet.getRow(38).getCell(0).getStringCellValue())
                    .isEqualTo("計");
            assertThat(sheet.getRow(38).getCell(5).getNumericCellValue())
                    .isEqualTo(350000d);
            assertThat(sheet.getRow(38).getCell(29).getNumericCellValue())
                    .isEqualTo(280000d);
        }
    }

    private Map<String, Object> row(int index) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("target_month", Date.valueOf(LocalDate.of(2026, 7, 1)));
        row.put("payment_date", Date.valueOf(LocalDate.of(2026, 8, 15)));
        row.put("company_name", "テスト株式会社");
        row.put("employee_name", "従業員" + index);
        row.put("work_day_count", 20);
        row.put("paid_leave_days", BigDecimal.ONE);
        row.put("overtime_hours", BigDecimal.valueOf(2));
        row.put("night_work_hours", BigDecimal.ONE);
        row.put("basic_salary", BigDecimal.valueOf(10000));
        row.put("overtime_pay_amount", BigDecimal.valueOf(1000));
        row.put("night_pay_amount", BigDecimal.valueOf(500));
        row.put("driver_allowance_amount", BigDecimal.valueOf(100));
        row.put("other_allowance_amount", BigDecimal.valueOf(100));
        row.put("business_trip_allowance_amount", BigDecimal.valueOf(100));
        row.put("gross_amount", BigDecimal.valueOf(11800));
        row.put("health_insurance", BigDecimal.valueOf(500));
        row.put("child_care_contribution", BigDecimal.ZERO);
        row.put("pension_insurance", BigDecimal.valueOf(500));
        row.put("employment_insurance", BigDecimal.valueOf(100));
        row.put("social_insurance_total", BigDecimal.valueOf(1100));
        row.put("taxable_amount", BigDecimal.valueOf(10700));
        row.put("income_tax", BigDecimal.valueOf(500));
        row.put("year_end_adjustment_amount", BigDecimal.ZERO);
        row.put("resident_tax", BigDecimal.valueOf(500));
        row.put("dormitory_fee_amount", BigDecimal.valueOf(100));
        row.put("mobile_rental_amount", BigDecimal.valueOf(100));
        row.put("wifi_fee_amount", BigDecimal.valueOf(100));
        row.put("other_deduction_amount", BigDecimal.valueOf(100));
        row.put("deduction_total", BigDecimal.valueOf(2400));
        row.put("net_before_advance_amount", BigDecimal.valueOf(9400));
        row.put("advance_payment_amount", BigDecimal.valueOf(1000));
        row.put("saving_amount", BigDecimal.valueOf(400));
        row.put("net_payment_amount", BigDecimal.valueOf(8000));
        return row;
    }
}
