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

    @Test
    void rendersDailyPaySlipDataPreview() throws Exception {
        String html = render(
                "daily_pay_slip.html",
                Map.ofEntries(
                        Map.entry("payment_date", "2026-08-01"),
                        Map.entry("employee_code", "E001"),
                        Map.entry("employee_name", "山田太郎"),
                        Map.entry("labor_period_from", "2026-08-01"),
                        Map.entry("labor_period_to", "2026-08-01"),
                        Map.entry("work_hours", amount("8")),
                        Map.entry("overtime_hours", amount("1")),
                        Map.entry("night_work_hours", amount("0")),
                        Map.entry("basic_salary", amount("10000")),
                        Map.entry("allowance_item_name1", "運転手当"),
                        Map.entry("allowance_item_value1", amount("1000")),
                        Map.entry("deduction_item_name1", "前借り"),
                        Map.entry("deduction_item_value1", amount("2000")),
                        Map.entry("gross_amount", amount("11000")),
                        Map.entry("deduction_total", amount("2000")),
                        Map.entry("net_payment_amount", amount("9000")),
                        Map.entry("note", "確認済み")
                )
        );

        assertThat(html)
                .contains("日次給与明細")
                .contains("山田太郎")
                .contains("運転手当")
                .contains("前借り")
                .contains("9,000 円")
                .doesNotContain("th:text");
    }

    @Test
    void rendersDailyPaySlipEmptyState() throws Exception {
        String html = renderRows("daily_pay_slip.html", List.of());

        assertThat(html)
                .contains("対象日に日次給与明細のデータがありません。")
                .doesNotContain("th:if");
    }

    private String render(
            String fileName,
            Map<String, Object> row
    ) throws Exception {
        return renderRows(fileName, List.of(row));
    }

    private String renderRows(
            String fileName,
            List<Map<String, Object>> rows
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
                        "rows", rows,
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
