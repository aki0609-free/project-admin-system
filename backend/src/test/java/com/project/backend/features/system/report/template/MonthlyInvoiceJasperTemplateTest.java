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
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

class MonthlyInvoiceJasperTemplateTest {

    @Test
    void compilesAndRendersAllCustomerInvoicePatterns() throws Exception {
        for (int pattern = 1; pattern <= 3; pattern++) {
            String template = "reports/monthly_invoice_pattern_"
                    + pattern + ".jrxml";

            try (InputStream input = getClass().getClassLoader()
                    .getResourceAsStream(template)) {
                assertThat(input).as(template).isNotNull();
                var report = JasperCompileManager.compileReport(input);
                List<Map<String, ?>> rows = sampleRows(pattern);

                @SuppressWarnings({ "rawtypes", "unchecked" })
                var dataSource = new JRMapCollectionDataSource((List) rows);
                JasperPrint print = JasperFillManager.fillReport(
                        report,
                        new LinkedHashMap<>(),
                        dataSource
                );

                assertThat(print.getPageWidth()).isEqualTo(842);
                assertThat(print.getPageHeight()).isEqualTo(595);
                assertThat(print.getPages()).hasSizeGreaterThanOrEqualTo(2);

                byte[] pdf = JasperExportManager.exportReportToPdf(print);
                assertThat(pdf).startsWith("%PDF".getBytes())
                        .hasSizeGreaterThan(10_000);

                Path output = Path.of(
                        "build", "reports", "jasper",
                        "monthly_invoice_pattern_" + pattern + "-sample.pdf"
                );
                Files.createDirectories(output.getParent());
                Files.write(output, pdf);
            }
        }
    }

    private List<Map<String, ?>> sampleRows(int pattern) {
        List<Map<String, ?>> rows = new ArrayList<>();
        int rowCount = pattern == 1 ? 12 : 10;
        for (int index = 0; index < rowCount; index++) {
            Map<String, Object> row = commonRow();
            row.put("work_date", Date.valueOf(
                    LocalDate.of(2026, 6, 1).plusDays(index)
            ));
            row.put("job_name", index % 2 == 0 ? "一般塗装工" : "職長塗装工");
            row.put("site_name", index < 5 ? "御成橋" : "向田橋");
            row.put("site_role_name", index % 2 == 0 ? "一般" : "職長");
            row.put("base_quantity", amount("1"));
            row.put("base_unit_price", amount("23000"));
            row.put("base_amount", amount("23000"));
            row.put("overtime_hours", amount("2"));
            row.put("overtime_unit_price", amount("3594"));
            row.put("overtime_amount", amount("7188"));
            row.put("night_hours", amount("0"));
            row.put("night_unit_price", amount("4313"));
            row.put("night_amount", amount("0"));
            row.put("holiday_hours", amount("0"));
            row.put("holiday_unit_price", amount("4313"));
            row.put("holiday_amount", amount("0"));
            row.put("commute_amount", amount("1560"));
            row.put("line_amount", amount("31748"));
            row.put("metric_order", index % 5 + 1);
            row.put("metric_name", List.of(
                    "通常", "残業", "深夜", "休日", "通勤"
            ).get(index % 5));
            row.put("metric_total", amount("10"));
            row.put("unit_price", amount("23000"));
            row.put("metric_amount", amount("230000"));
            for (int day = 1; day <= 31; day++) {
                row.put(
                        "day_%02d".formatted(day),
                        day <= 20 && day % 3 == index % 3
                                ? amount("1")
                                : amount("0")
                );
            }
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> commonRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("invoice_history_id", 1L);
        row.put("invoice_number", "202606-000001-V1");
        row.put("issue_date", Date.valueOf("2026-06-25"));
        row.put("target_month", Date.valueOf("2026-06-01"));
        row.put("customer_name", "株式会社 池ノ沢工業");
        row.put("company_name", "株式会社 富陽");
        row.put("company_postal_code", "416-0909");
        row.put("company_address", "静岡県富士市松岡1162-1");
        row.put("company_phone", "0120-49-2401");
        row.put("company_fax", "0120-49-8601");
        row.put("qualified_invoice_issuer_number", "T8021001057111");
        row.put("bank_display_text", "スルガ銀行 中央林間支店 普通 3498864");
        row.put("invoice_note", "振込手数料は御社負担にてお願いいたします。");
        row.put("tax_rate", amount("0.10"));
        row.put("subtotal_amount", amount("2417850"));
        row.put("tax_amount", amount("241785"));
        row.put("total_amount", amount("2659635"));
        return row;
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
