package com.project.backend.features.operation.reportpreview.config;

import java.util.ArrayList;
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

        List<String> issues = validateActiveDefinitions();
        if (!issues.isEmpty()) {
            throw new IllegalStateException(
                    "帳票プレビュー定義に不整合があります。\n - "
                            + String.join("\n - ", issues)
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

    /**
     * 台帳専用画面を除く全有効定義について、DB内だけで確定できる依存関係を検証する。
     * S3/LocalのTemplateは一時的なStorage障害でアプリ全体を停止させないため、
     * 実際のPreview・出力時に個別に検証する。
     */
    private List<String> validateActiveDefinitions() {
        List<DefinitionReadiness> definitions = jdbcTemplate.query("""
                select
                    preview.id,
                    preview.tenant_id,
                    preview.operation_type,
                    preview.report_code,
                    preview.output_type,
                    preview.job_code,
                    preview.table_name,
                    coalesce(
                        nullif(trim(preview.filter_column_name), ''),
                        case preview.operation_type
                            when 'MONTHLY' then 'target_month'
                            when 'DAILY' then 'payment_date'
                            else 'target_date'
                        end
                    ) as resolved_filter_column,
                    (
                        select count(*)
                        from information_schema.tables source_table
                        where source_table.table_schema = database()
                          and source_table.table_name = preview.table_name
                    ) as source_count,
                    (
                        select count(*)
                        from information_schema.columns filter_column
                        where filter_column.table_schema = database()
                          and filter_column.table_name = preview.table_name
                          and filter_column.column_name = coalesce(
                              nullif(trim(preview.filter_column_name), ''),
                              case preview.operation_type
                                  when 'MONTHLY' then 'target_month'
                                  when 'DAILY' then 'payment_date'
                                  else 'target_date'
                              end
                          )
                    ) as filter_column_count,
                    (
                        select count(*)
                        from operation_report_preview_column preview_column
                        left join information_schema.columns source_column
                          on source_column.table_schema = database()
                         and source_column.table_name = preview.table_name
                         and source_column.column_name = preview_column.column_name
                        where preview_column.operation_report_preview_id = preview.id
                          and preview_column.active_flag = true
                          and preview_column.deleted_at is null
                          and source_column.column_name is null
                    ) as missing_preview_column_count,
                    (
                        select count(*)
                        from batch_job_definition job
                        where job.tenant_id = preview.tenant_id
                          and job.job_code = preview.job_code
                          and job.active_flag = true
                          and job.deleted_at is null
                    ) as active_job_count
                from operation_report_preview preview
                where preview.active_flag = true
                  and preview.deleted_at is null
                  and preview.operation_type <> 'BOOK'
                order by preview.tenant_id, preview.operation_type,
                         preview.display_order, preview.id
                """, (resultSet, rowNumber) -> new DefinitionReadiness(
                resultSet.getLong("id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("operation_type"),
                resultSet.getString("report_code"),
                resultSet.getString("output_type"),
                resultSet.getString("job_code"),
                resultSet.getString("table_name"),
                resultSet.getString("resolved_filter_column"),
                resultSet.getInt("source_count"),
                resultSet.getInt("filter_column_count"),
                resultSet.getInt("missing_preview_column_count"),
                resultSet.getInt("active_job_count")
        ));

        return validateDefinitions(definitions);
    }

    List<String> validateDefinitions(List<DefinitionReadiness> definitions) {
        List<String> issues = new ArrayList<>();
        for (DefinitionReadiness definition : definitions) {
            String label = definition.tenantId() + "/"
                    + definition.operationType() + "/"
                    + definition.reportCode() + "(id=" + definition.id() + ")";

            if (definition.sourceCount() == 0) {
                issues.add(label + ": データ元View/Tableが存在しません: "
                        + definition.tableName());
                continue;
            }
            if (definition.filterColumnCount() == 0) {
                issues.add(label + ": 対象日・月の絞込列が存在しません: "
                        + definition.resolvedFilterColumn());
            }
            if (definition.missingPreviewColumnCount() > 0) {
                issues.add(label + ": データ元に存在しない表示列が "
                        + definition.missingPreviewColumnCount() + " 件あります");
            }
            if (requiresBatchJob(definition.outputType())) {
                if (definition.jobCode() == null || definition.jobCode().isBlank()) {
                    issues.add(label + ": 出力用Job Codeが未設定です");
                } else if (definition.activeJobCount() == 0) {
                    issues.add(label + ": 有効な出力Jobが存在しません: "
                            + definition.jobCode());
                }
            }
        }
        return issues;
    }

    private boolean requiresBatchJob(String outputType) {
        return "PDF".equals(outputType)
                || "CSV".equals(outputType)
                || "EXCEL".equals(outputType)
                || "CUSTOM".equals(outputType);
    }

    record DefinitionReadiness(
            long id,
            String tenantId,
            String operationType,
            String reportCode,
            String outputType,
            String jobCode,
            String tableName,
            String resolvedFilterColumn,
            int sourceCount,
            int filterColumnCount,
            int missingPreviewColumnCount,
            int activeJobCount
    ) {
    }
}
