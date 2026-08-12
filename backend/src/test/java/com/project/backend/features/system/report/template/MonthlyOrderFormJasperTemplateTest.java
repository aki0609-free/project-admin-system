package com.project.backend.features.system.report.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

class MonthlyOrderFormJasperTemplateTest {

    @Test
    void monthlyOrderFormTemplateCompilesAndRendersOnePage() throws Exception {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("reports/monthly_order_form.jrxml")) {
            assertThat(input).isNotNull();
            var report = JasperCompileManager.compileReport(input);
            assertThat(report.getName()).isEqualTo("monthly_order_form");

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order_number", "ORD-202607-000020-V1");
            row.put("order_date", Date.valueOf("2026-06-05"));
            row.put("contract_from", Date.valueOf("2026-06-05"));
            row.put("contract_to", Date.valueOf("2026-07-20"));
            row.put("subject_text", "2026年07月 業務請負");
            row.put("work_description", "作業内容は、口頭及び書面にて説明");
            row.put("subcontractor_name", "株式会社富陽");
            row.put("subcontractor_postal_code", "416-0909");
            row.put("subcontractor_address", "静岡県富士市松岡1162-1");
            row.put("prime_contractor_name", "合同アスファルト株式会社");
            row.put("prime_contractor_postal_code", "100-0001");
            row.put("prime_contractor_address", "東京都千代田区千代田1-1");
            row.put("show_prime_contractor", true);
            row.put("construction_price", new BigDecimal("690703"));
            row.put("tax_amount", new BigDecimal("69070"));
            row.put("contract_amount", new BigDecimal("759773"));

            @SuppressWarnings({"rawtypes", "unchecked"})
            var dataSource = new JRMapCollectionDataSource((List) List.of(row));
            var print = JasperFillManager.fillReport(
                    report,
                    new LinkedHashMap<>(),
                    dataSource
            );
            assertThat(print.getPages()).hasSize(1);
            byte[] pdf = JasperExportManager.exportReportToPdf(print);
            assertThat(pdf).startsWith("%PDF".getBytes()).hasSizeGreaterThan(8_000);

            Path output = Path.of(
                    "build", "reports", "jasper", "monthly_order_form-sample.pdf"
            );
            Files.createDirectories(output.getParent());
            Files.write(output, pdf);
        }
    }
}
