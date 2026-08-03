package com.project.backend.features.system.report.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

class MonthlyPaySlipJasperTemplateTest {

    private static final String TEMPLATE =
            "reports/monthly_pay_slip.jrxml";

    @Test
    void compilesAndRendersOnePagePerEmployee() throws Exception {
        JasperReport report;

        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream(TEMPLATE)) {
            assertThat(input)
                    .as("月次給与明細JRXMLがclasspathに存在すること")
                    .isNotNull();
            report = JasperCompileManager.compileReport(input);
        }

        List<Map<String, ?>> rows = List.of(
                sampleRow("E0001", "富陽 太郎", 1),
                sampleRow("E0002", "富陽 花子", 1)
        );

        @SuppressWarnings({ "rawtypes", "unchecked" })
        JRMapCollectionDataSource dataSource =
                new JRMapCollectionDataSource((List) rows);

        JasperPrint print = JasperFillManager.fillReport(
                report,
                new LinkedHashMap<>(),
                dataSource
        );

        assertThat(print.getPages()).hasSize(2);
        assertThat(print.getPageWidth()).isEqualTo(729);
        assertThat(print.getPageHeight()).isEqualTo(516);

        byte[] pdf = JasperExportManager.exportReportToPdf(print);
        assertThat(pdf)
                .startsWith("%PDF".getBytes())
                .hasSizeGreaterThan(10_000);

        Path output = Path.of(
                "build",
                "reports",
                "jasper",
                "monthly_pay_slip-sample.pdf"
        );
        Files.createDirectories(output.getParent());
        Files.write(output, pdf);
    }

    private Map<String, Object> sampleRow(
            String employeeCode,
            String employeeName,
            int closingVersion
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("target_month", Date.valueOf(LocalDate.of(2026, 7, 1)));
        row.put("closing_version", closingVersion);
        row.put("employee_code", employeeCode);
        row.put("employee_name", employeeName);
        row.put("company_name", "株式会社富陽");
        row.put("period_from", Date.valueOf(LocalDate.of(2026, 6, 21)));
        row.put("period_to", Date.valueOf(LocalDate.of(2026, 7, 20)));
        row.put("work_day_count", 21);
        row.put("work_hours", amount("168"));
        row.put("overtime_hours", amount("12.5"));
        row.put("night_work_hours", amount("2"));
        row.put("holiday_work_hours", amount("8"));
        row.put("paid_leave_days", amount("1"));
        row.put("basic_salary", amount("300000"));
        row.put("allowance_total", amount("35000"));
        row.put("gross_amount", amount("335000"));
        row.put("health_insurance", amount("16600"));
        row.put("child_care_contribution", amount("0"));
        row.put("pension_insurance", amount("27450"));
        row.put("employment_insurance", amount("1843"));
        row.put("social_insurance_total", amount("45893"));
        row.put("taxable_amount", amount("289107"));
        row.put("income_tax", amount("9600"));
        row.put("resident_tax", amount("12000"));
        row.put("legal_deduction_total", amount("67493"));
        row.put("other_deduction_total", amount("23000"));
        row.put("deduction_total", amount("90493"));
        row.put("advance_payment_amount", amount("20000"));
        row.put("net_payment_amount", amount("244507"));

        List<String> allowanceNames = List.of(
                "勤務態度手当",
                "運転手当",
                "管理手当",
                "現場手当"
        );
        List<BigDecimal> allowanceValues = List.of(
                amount("10000"),
                amount("5000"),
                amount("15000"),
                amount("5000")
        );
        putItems(row, "allowance", allowanceNames, allowanceValues);

        putItems(
                row,
                "legal",
                List.of("法定追加控除"),
                List.of(amount("0"))
        );

        putItems(
                row,
                "other",
                List.of("前払い", "寮費", "Wi-Fi使用料"),
                List.of(amount("20000"), amount("2000"), amount("1000"))
        );

        return row;
    }

    private void putItems(
            Map<String, Object> row,
            String prefix,
            List<String> names,
            List<BigDecimal> values
    ) {
        List<String> paddedNames = new ArrayList<>(names);
        List<BigDecimal> paddedValues = new ArrayList<>(values);

        while (paddedNames.size() < 12) {
            paddedNames.add(null);
            paddedValues.add(null);
        }

        for (int index = 0; index < 12; index++) {
            String no = "%02d".formatted(index + 1);
            row.put(prefix + "_item_name_" + no, paddedNames.get(index));
            row.put(prefix + "_item_value_" + no, paddedValues.get(index));
        }
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
