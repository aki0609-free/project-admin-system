package com.project.backend.features.system.report.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

class DailyWorkOrderJasperTemplateTest {

    private static final String TEMPLATE = "reports/daily_work_order.jrxml";

    @Test
    void rendersOneLandscapePageForEachCustomerSitePage() throws Exception {
        var report = compileTemplate();
        @SuppressWarnings({ "rawtypes", "unchecked" })
        var dataSource = new JRMapCollectionDataSource((List) List.of(
                samplePage("株式会社サンプル建設", "横浜駅前再開発現場", 1L, 2L),
                samplePage("株式会社サンプル建設", "横浜駅前再開発現場", 2L, 2L)
        ));

        var print = JasperFillManager.fillReport(
                report,
                new LinkedHashMap<>(),
                dataSource
        );

        assertThat(print.getPageWidth()).isEqualTo(842);
        assertThat(print.getPageHeight()).isEqualTo(595);
        assertThat(print.getPages()).hasSize(2);

        byte[] pdf = JasperExportManager.exportReportToPdf(print);
        assertThat(pdf).startsWith("%PDF".getBytes()).hasSizeGreaterThan(10_000);

        Path output = Path.of(
                "build", "reports", "jasper", "daily_work_order-sample.pdf"
        );
        Files.createDirectories(output.getParent());
        Files.write(output, pdf);
    }

    private net.sf.jasperreports.engine.JasperReport compileTemplate() throws Exception {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(TEMPLATE)) {
            assertThat(input).isNotNull();
            return JasperCompileManager.compileReport(input);
        }
    }

    private Map<String, Object> samplePage(
            String customerName,
            String siteName,
            long pageNo,
            long totalPages
    ) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("target_date", Date.valueOf(LocalDate.of(2026, 8, 3)));
        row.put("weekday_label", "月");
        row.put("customer_name", customerName);
        row.put("site_name", siteName);
        row.put("page_no", pageNo);
        row.put("total_pages", totalPages);
        row.put("page_worker_count", pageNo == 1 ? 10L : 3L);
        row.put("distance_from_company_km", 18);
        row.put("vehicle_count", 2);
        row.put("company_name", "株式会社 富陽");
        row.put("company_phone", "0120-49-2401");
        row.put("company_fax", "0120-49-8601");

        for (int index = 1; index <= 10; index++) {
            boolean present = pageNo == 1 || index <= 3;
            row.put(
                    "employee_name_" + index,
                    present ? "作業員 " + ((pageNo - 1) * 10 + index) : null
            );
            row.put(
                    "work_description_" + index,
                    present ? "内装解体・搬出作業" : null
            );
        }
        return row;
    }
}
