package com.project.backend.features.operation.reportpreview.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

    private DefaultApplicationArguments arguments() {
        return new DefaultApplicationArguments(new String[0]);
    }
}
