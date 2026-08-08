package com.project.backend.features.operation.reportpreview.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 日次画面を公開する環境で、必要なViewと帳票定義の欠落を起動時に検出する。
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 50)
@ConditionalOnProperty(
        prefix = "app.report.preview.readiness-check",
        name = "enabled",
        havingValue = "true"
)
public class OperationReportPreviewReadinessValidator
        implements ApplicationRunner {

    private static final List<String> REQUIRED_VIEWS = List.of(
            "vw_daily_labor_cost_preview",
            "vw_daily_payment_preparation_preview"
    );
    private static final List<String> REQUIRED_REPORT_CODES = List.of(
            "DAILY_LABOR_COST_PREVIEW",
            "DAILY_PAYMENT_PREPARATION"
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int viewCount = countRequiredViews();
        int definitionCount = countRequiredDefinitions();

        if (viewCount != REQUIRED_VIEWS.size()
                || definitionCount != REQUIRED_REPORT_CODES.size()) {
            throw new IllegalStateException(
                    "日次帳票プレビュー基盤が未適用です。"
                            + " views=" + viewCount + "/" + REQUIRED_VIEWS.size()
                            + ", definitions=" + definitionCount + "/"
                            + REQUIRED_REPORT_CODES.size()
            );
        }
    }

    private int countRequiredViews() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.views
                where table_schema = database()
                  and table_name in (?, ?)
                """,
                Integer.class,
                REQUIRED_VIEWS.get(0),
                REQUIRED_VIEWS.get(1)
        );
        return count == null ? 0 : count;
    }

    private int countRequiredDefinitions() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(distinct report_code)
                from operation_report_preview
                where tenant_id = 'default'
                  and active_flag = true
                  and deleted_at is null
                  and report_code in (?, ?)
                """,
                Integer.class,
                REQUIRED_REPORT_CODES.get(0),
                REQUIRED_REPORT_CODES.get(1)
        );
        return count == null ? 0 : count;
    }
}
