package com.project.backend.features.operation.reportpreview.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

class OperationReportPreviewReadinessValidatorTest {

    private final JdbcTemplate jdbcTemplate =
            Mockito.mock(JdbcTemplate.class);
    private final OperationReportPreviewReadinessValidator validator =
            new OperationReportPreviewReadinessValidator(jdbcTemplate);

    @Test
    void acceptsCompleteDailyPreviewFoundation() {
        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(),
                any()
        )).thenReturn(2, 2);

        assertThatCode(() -> validator.run(arguments()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingViewOrDefinition() {
        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                any(),
                any()
        )).thenReturn(1, 2);

        assertThatThrownBy(() -> validator.run(arguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("日次帳票プレビュー基盤が未適用")
                .hasMessageContaining("views=1/2")
                .hasMessageContaining("definitions=2/2");
    }

    @Test
    void reportsEveryInvalidDependencyForAnActiveDefinition() {
        var definition = new OperationReportPreviewReadinessValidator.DefinitionReadiness(
                10L,
                "default",
                "MONTHLY",
                "MONTHLY_PAY_SLIP",
                "PDF",
                "PRINT_MONTHLY_PAY_SLIP",
                "vw_monthly_pay_slip_operation_preview",
                "target_month",
                1,
                0,
                2,
                0
        );

        List<String> issues = validator.validateDefinitions(List.of(definition));

        org.assertj.core.api.Assertions.assertThat(issues)
                .hasSize(3)
                .allMatch(issue -> issue.contains(
                        "default/MONTHLY/MONTHLY_PAY_SLIP(id=10)"))
                .anyMatch(issue -> issue.contains("絞込列"))
                .anyMatch(issue -> issue.contains("表示列が 2 件"))
                .anyMatch(issue -> issue.contains("有効な出力Job"));
    }

    @Test
    void doesNotRequireBatchJobForHtmlPreview() {
        var definition = new OperationReportPreviewReadinessValidator.DefinitionReadiness(
                11L,
                "default",
                "DAILY",
                "DAILY_LABOR_COST_PREVIEW",
                "HTML_PREVIEW",
                null,
                "vw_daily_labor_cost_preview",
                "payment_date",
                1,
                1,
                0,
                0
        );

        org.assertj.core.api.Assertions.assertThat(
                validator.validateDefinitions(List.of(definition)))
                .isEmpty();
    }

    private DefaultApplicationArguments arguments() {
        return new DefaultApplicationArguments(new String[0]);
    }
}
