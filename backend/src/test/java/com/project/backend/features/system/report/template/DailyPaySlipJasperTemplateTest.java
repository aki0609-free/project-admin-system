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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

class DailyPaySlipJasperTemplateTest {

    private static final String TEMPLATE = "reports/daily_pay_slip.jrxml";

    @Test
    void rendersTwoEmployeesPerB5PortraitPage() throws Exception {
        JasperReport report;
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(TEMPLATE)) {
            assertThat(input).isNotNull();
            report = JasperCompileManager.compileReport(input);
        }

        List<Map<String, ?>> rows = List.of(
                sampleRow("E0001", "富陽 太郎"),
                sampleRow("E0002", "富陽 花子"),
                sampleRow("E0003", "富陽 次郎")
        );

        @SuppressWarnings({ "rawtypes", "unchecked" })
        var dataSource = new JRMapCollectionDataSource((List) rows);
        JasperPrint print = JasperFillManager.fillReport(
                report,
                new LinkedHashMap<>(),
                dataSource
        );

        assertThat(print.getPageWidth()).isEqualTo(516);
        assertThat(print.getPageHeight()).isEqualTo(729);
        assertThat(print.getPages()).hasSize(2);

        byte[] pdf = JasperExportManager.exportReportToPdf(print);
        assertThat(pdf).startsWith("%PDF".getBytes()).hasSizeGreaterThan(8_000);
        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text)
                    .contains("〒123-4567 東京都千代田区テスト1-2-3")
                    .contains("2 日")
                    .contains("法定準備金（累計額：5,000円）")
                    .doesNotContain("加算計");
        }

        Path output = Path.of(
                "build", "reports", "jasper", "daily_pay_slip-sample.pdf"
        );
        Files.createDirectories(output.getParent());
        Files.write(output, pdf);
    }

    private Map<String, Object> sampleRow(
            String employeeCode,
            String employeeName
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        LocalDate paymentDate = LocalDate.of(2026, 7, 30);
        row.put("payment_date", Date.valueOf(paymentDate));
        row.put("employee_code", employeeCode);
        row.put("employee_name", employeeName);
        row.put("employee_address", "〒123-4567 東京都千代田区テスト1-2-3");
        row.put("labor_period_from", Date.valueOf(paymentDate.minusDays(1)));
        row.put("labor_period_to", Date.valueOf(paymentDate));
        row.put("attendance_days", 2);
        row.put("work_hours", amount("8"));
        row.put("overtime_hours", amount("1.5"));
        row.put("night_work_hours", amount("0"));
        row.put("work_hours_label", "8時間");
        row.put("overtime_hours_label", "1時間30分");
        row.put("night_work_hours_label", "0分");
        row.put("basic_salary", amount("10000"));
        row.put("allowance_total", amount("1500"));
        row.put("deduction_total", amount("500"));
        row.put("gross_amount", amount("11500"));
        row.put("daily_payment_amount", amount("11000"));
        row.put("net_payment_amount", amount("11000"));
        row.put("legal_deposit_balance", amount("5000"));
        row.put("loan_balance", amount("20000"));
        row.put("saving_balance", amount("30000"));
        row.put("note", "月次給与から前払い額として精算");

        putItems(
                row,
                "allowance",
                List.of("早出残業手当", "勤務態度手当"),
                List.of(amount("1000"), amount("500"))
        );
        putItems(
                row,
                "deduction",
                List.of(
                        "法定準備金（累計額：5,000円）",
                        "貯金（累計額：30,000円）"
                ),
                List.of(amount("500"), amount("0"))
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
        while (paddedNames.size() < 10) {
            paddedNames.add(null);
            paddedValues.add(null);
        }
        for (int index = 0; index < 10; index++) {
            row.put(prefix + "_item_name" + (index + 1), paddedNames.get(index));
            row.put(prefix + "_item_value" + (index + 1), paddedValues.get(index));
        }
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
