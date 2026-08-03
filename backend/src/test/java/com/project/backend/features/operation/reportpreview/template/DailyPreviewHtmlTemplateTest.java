package com.project.backend.features.operation.reportpreview.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.project.backend.features.system.report.service.core.ReportHtmlTemplateRenderer;

class DailyPreviewHtmlTemplateTest {

    private final ReportHtmlTemplateRenderer renderer =
            new ReportHtmlTemplateRenderer();

    @Test
    void rendersDailyLaborCostTemplate() throws Exception {
        String html = render(
                "daily_labor_cost.html",
                Map.of(
                        "work_date_label", "2026年08月01日",
                        "employee_code", "E001",
                        "employee_name", "山田太郎",
                        "payment_cycle", "DAILY",
                        "gross_payment_amount", amount("12000"),
                        "payment_amount", amount("10000"),
                        "total_gross_payment_amount", amount("12000"),
                        "total_payment_amount", amount("10000")
                )
        );

        assertThat(html)
                .contains("日別労務費一覧")
                .contains("山田太郎")
                .contains("12,000 円")
                .doesNotContain("th:text");
    }

    @Test
    void rendersDailyPaymentPreparationTemplate() throws Exception {
        String html = render(
                "daily_payment_preparation.html",
                Map.ofEntries(
                        Map.entry("payment_date_label", "2026年08月01日"),
                        Map.entry("employee_code", "E001"),
                        Map.entry("employee_name", "山田太郎"),
                        Map.entry("payment_cycle", "DAILY"),
                        Map.entry("gross_payment_amount", amount("12000")),
                        Map.entry("allowance_amount", amount("1000")),
                        Map.entry("deduction_amount", amount("2000")),
                        Map.entry("net_payment_amount", amount("10000")),
                        Map.entry("total_net_payment_amount", amount("10000")),
                        Map.entry("bill_10000", 1),
                        Map.entry("bill_5000", 0),
                        Map.entry("bill_1000", 0),
                        Map.entry("coin_500", 0),
                        Map.entry("coin_100", 0),
                        Map.entry("coin_50", 0)
                )
        );

        assertThat(html)
                .contains("給与支払表")
                .contains("山田太郎")
                .contains("10,000 円")
                .doesNotContain("th:text");
    }

    private String render(
            String fileName,
            Map<String, Object> row
    ) throws Exception {
        ClassPathResource resource = new ClassPathResource(
                "templates/operation/reportpreview/" + fileName
        );
        String source;
        try (var inputStream = resource.getInputStream()) {
            source = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }

        return renderer.render(
                source,
                Map.of(
                        "rows", List.of(row),
                        "columns", List.of(),
                        "definition", Map.of(),
                        "request", Map.of()
                )
        );
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
