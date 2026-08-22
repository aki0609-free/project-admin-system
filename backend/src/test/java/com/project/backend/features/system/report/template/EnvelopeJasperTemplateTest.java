package com.project.backend.features.system.report.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

class EnvelopeJasperTemplateTest {

    @Test
    void compilesAndRendersNaga3AndKaku2Templates() throws Exception {
        assertTemplate("envelope_naga3", 666, 340);
        assertTemplate("envelope_kaku2", 941, 680);
    }

    private void assertTemplate(
            String templateName,
            int expectedWidth,
            int expectedHeight
    ) throws Exception {
        String resourceName = "reports/" + templateName + ".jrxml";
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(resourceName)) {
            assertThat(input).as(resourceName).isNotNull();
            var report = JasperCompileManager.compileReport(input);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("customerName", "株式会社サンプル建設");
            row.put("postalCode", "416-0909");
            row.put("address", "静岡県富士市松岡1162-1");
            row.put("stamp", "請求書在中");
            row.put("honorific", "御中");

            @SuppressWarnings({ "rawtypes", "unchecked" })
            var dataSource = new JRMapCollectionDataSource((List) List.of(row));
            var print = JasperFillManager.fillReport(
                    report,
                    new LinkedHashMap<>(),
                    dataSource
            );

            assertThat(print.getPageWidth()).isEqualTo(expectedWidth);
            assertThat(print.getPageHeight()).isEqualTo(expectedHeight);
            assertThat(print.getPages()).hasSize(1);

            byte[] pdf = JasperExportManager.exportReportToPdf(print);
            assertThat(pdf).startsWith("%PDF".getBytes()).hasSizeGreaterThan(3_000);

            Path output = Path.of(
                    "build", "reports", "jasper", templateName + "-sample.pdf"
            );
            Files.createDirectories(output.getParent());
            Files.write(output, pdf);
        }
    }
}
