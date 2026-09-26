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
                .contains("background: #f8fafc")
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
                        Map.entry("coin_50", 0),
                        Map.entry("coin_10", 0),
                        Map.entry("coin_5", 0),
                        Map.entry("coin_1", 0)
                )
        );

        assertThat(html)
                .contains("給与支払表")
                .contains("山田太郎")
                .contains("10,000 円")
                .contains("10円玉")
                .contains("5円玉")
                .contains("1円玉")
                .doesNotContain("th:text");
    }

    @Test
    void rendersDailyPaySlipDataPreview() throws Exception {
        String html = render(
                "daily_pay_slip.html",
                Map.ofEntries(
                        Map.entry("payment_date_label", "2026年8月1日"),
                        Map.entry("employee_code", "E001"),
                        Map.entry("employee_name", "山田太郎"),
                        Map.entry("employee_address", "〒123-4567 東京都千代田区テスト1-2-3"),
                        Map.entry("labor_period_from_label", "2026年8月1日"),
                        Map.entry("labor_period_to_label", "2026年8月1日"),
                        Map.entry("attendance_days", 1),
                        Map.entry("work_hours", amount("8")),
                        Map.entry("overtime_hours", amount("1")),
                        Map.entry("night_work_hours", amount("0")),
                        Map.entry("work_hours_label", "8時間"),
                        Map.entry("overtime_hours_label", "1時間15分"),
                        Map.entry("night_work_hours_label", "0分"),
                        Map.entry("basic_salary", amount("10000")),
                        Map.entry("allowance_item_name1", "運転手当"),
                        Map.entry("allowance_item_value1", amount("1000")),
                        Map.entry("deduction_item_name1", "前借り（残高：20,000円）"),
                        Map.entry("deduction_item_value1", amount("2000")),
                        Map.entry("gross_amount", amount("11000")),
                        Map.entry("deduction_total", amount("2000")),
                        Map.entry("net_payment_amount", amount("9000")),
                        Map.entry("note", "確認済み")
                )
        );

        assertThat(html)
                .contains("支払明細書")
                .contains("山田太郎")
                .contains("2026年8月1日")
                .contains("8時間")
                .contains("1時間15分")
                .contains("出勤日数")
                .contains("1 日")
                .contains("運転手当")
                .contains("前借り")
                .contains("前借り（残高：20,000円）")
                .contains("9,000 円")
                .containsOnlyOnce("控除合計")
                .doesNotContain("支給合計")
                .doesNotContain("住所：")
                .doesNotContain("※上記金額を受領しました。")
                .contains("grid-template-columns: 230px 1fr 230px 1fr")
                .contains("white-space: nowrap")
                .doesNotContain("th:text");
    }

    @Test
    void rendersDailyPaySlipEmptyState() throws Exception {
        String html = renderRows("daily_pay_slip.html", List.of());

        assertThat(html)
                .contains("対象日に支払明細書のデータがありません。")
                .doesNotContain("th:if");
    }

    @Test
    void rendersMonthlyPaySlipAsLatestDataPreview()
            throws Exception {
        String html = render(
                "monthly_pay_slip.html",
                Map.ofEntries(
                        Map.entry("target_month_label", "2026年8月"),
                        Map.entry("employee_code", "E2E-AUG-D-001"),
                        Map.entry("employee_name", "八月日次 青木 一郎"),
                        Map.entry("company_name", "E2E ローカル検証会社"),
                        Map.entry("period_start_label", "2026年8月1日"),
                        Map.entry("period_end_label", "2026年8月31日"),
                        Map.entry("work_day_count", 20),
                        Map.entry("work_hours_label", "160時間0分"),
                        Map.entry("overtime_hours_label", "4時間0分"),
                        Map.entry("night_work_hours_label", "1時間0分"),
                        Map.entry("holiday_work_hours_label", "0時間0分"),
                        Map.entry("paid_leave_days", amount("0")),
                        Map.entry("basic_salary", amount("240000")),
                        Map.entry("allowance_item_name_1", "早出・残業金額"),
                        Map.entry("allowance_item_value_1", amount("7500")),
                        Map.entry("allowance_item_name_2", "勤務態度手当"),
                        Map.entry("allowance_item_value_2", amount("1000")),
                        Map.entry("allowance_item_name_3", "運転手当"),
                        Map.entry("allowance_item_value_3", amount("2500")),
                        Map.entry("allowance_item_name_4", "管理手当"),
                        Map.entry("allowance_item_value_4", amount("3000")),
                        Map.entry("health_insurance", amount("0")),
                        Map.entry("child_care_contribution", amount("0")),
                        Map.entry("pension_insurance", amount("0")),
                        Map.entry("employment_insurance", amount("0")),
                        Map.entry("income_tax", amount("0")),
                        Map.entry("resident_tax", amount("0")),
                        Map.entry("deduction_item_name_1", "寮費"),
                        Map.entry("deduction_item_value_1", amount("20000")),
                        Map.entry("deduction_item_name_2", "前払い"),
                        Map.entry("deduction_item_value_2", amount("225875")),
                        Map.entry("gross_amount", amount("257875")),
                        Map.entry("tax_deduction_total", amount("0")),
                        Map.entry("other_deduction_total", amount("245875")),
                        Map.entry("deduction_total", amount("245875")),
                        Map.entry("net_amount", amount("12000")),
                        Map.entry("advance_payment_amount", amount("225875")),
                        Map.entry("legal_deposit_refund_amount", amount("-5000")),
                        Map.entry("taxable_amount", amount("251875")),
                        Map.entry("loan_balance", amount("0")),
                        Map.entry("saving_balance", amount("30000"))
                )
        );

        assertThat(html)
                .contains("2026年8月 給与明細書")
                .contains("160時間0分")
                .contains("法定準備金返済額")
                .contains("-5,000 円")
                .contains("勤務態度手当", "運転手当", "管理手当")
                .contains("right: 108px")
                .contains("bottom: 0")
                .contains("控除1（法定控除）")
                .contains("控除2（その他控除）")
                .contains("借入金残高")
                .contains("貯金残高")
                .doesNotContain("締め版")
                .doesNotContain("＜備考＞")
                .doesNotContain("月次締め時点の確定データ")
                .doesNotContain("保険計")
                .doesNotContain("支払日")
                .doesNotContain("前払い額（日払い累計）")
                .doesNotContain("th:text");
    }

    @Test
    void rendersMonthlyPaySlipEmptyState() throws Exception {
        String html = renderRows("monthly_pay_slip.html", List.of());

        assertThat(html)
                .contains("対象月の給与明細データがありません。")
                .doesNotContain("th:if");
    }

    @Test
    void rendersMonthlyInvoiceWithCommonCoverInformation()
            throws Exception {
        String html = render(
                "monthly_invoice.html",
                Map.ofEntries(
                        Map.entry("customer_name", "E2E 土木株式会社"),
                        Map.entry("invoice_date", java.sql.Date.valueOf("2026-09-20")),
                        Map.entry("invoice_number", "202609-000025-V4"),
                        Map.entry("invoice_note", "下記の通り御請求申し上げます。"),
                        Map.entry("qualified_invoice_issuer_number", "T1215121512151"),
                        Map.entry("bank_display_text", "レスリー銀行 セブ支店 普通 12151215"),
                        Map.entry("total_amount", amount("829876")),
                        Map.entry("work_date", java.sql.Date.valueOf("2026-08-01")),
                        Map.entry("site_name", "E2E 検証現場"),
                        Map.entry("job_name", "土木作業員"),
                        Map.entry("job_code", "CIVIL"),
                        Map.entry("site_role_name", "一般"),
                        Map.entry("site_role_code", "GENERAL"),
                        Map.entry("employee_code", "E2E-EMP-001"),
                        Map.entry("employee_name", "検証 太郎"),
                        Map.entry("billing_unit", "人日"),
                        Map.entry("billing_quantity", amount("1")),
                        Map.entry("work_hours", amount("8")),
                        Map.entry("overtime_hours", amount("0")),
                        Map.entry("night_work_hours", amount("0")),
                        Map.entry("holiday_work_hours", amount("0")),
                        Map.entry("mileage", amount("10")),
                        Map.entry("billing_base_unit_price", amount("23000")),
                        Map.entry("billing_overtime_unit_price", amount("3594")),
                        Map.entry("billing_night_unit_price", amount("4313")),
                        Map.entry("billing_holiday_unit_price", amount("0")),
                        Map.entry("billing_commuting_unit_price", amount("30")),
                        Map.entry("base_amount", amount("23000")),
                        Map.entry("overtime_amount", amount("0")),
                        Map.entry("night_amount", amount("0")),
                        Map.entry("holiday_amount", amount("0")),
                        Map.entry("commuting_amount", amount("300")),
                        Map.entry("subtotal_amount", amount("23300"))
                )
        );

        assertThat(html)
                .contains("請 求 書")
                .contains("E2E 土木株式会社　御中")
                .contains("￥829,876")
                .contains("2026年9月20日")
                .contains("レスリー銀行 セブ支店 普通 12151215")
                .contains("適格請求書登録番号　T1215121512151")
                .contains("請求書番号　202609-000025-V4")
                .contains("/api/system/company-profile/invoice-logo")
                .doesNotContain("th:text")
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
