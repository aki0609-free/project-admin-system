package com.project.backend.features.system.rule.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import com.project.backend.features.system.rule.exception.RuleExecutionException;
import com.project.backend.features.system.rule.exception.RuleExecutionStage;

class RuleExceptionHandlerTest {

    private final RuleExceptionHandler handler = new RuleExceptionHandler();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void executionFailure_shouldReturnGenericMessageAndTraceId() {
        MDC.put("traceId", "trace-123");

        var response = handler.handleExecutionFailure(
                new RuleExecutionException(
                        "DAILY_PAY",
                        RuleExecutionStage.DSL_EXECUTION,
                        "JexlException"
                )
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("RULE_EXECUTION_FAILED");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Ruleの実行に失敗しました。追跡IDを管理者へ連絡してください。");
        assertThat(response.getBody().getTraceId()).isEqualTo("trace-123");
    }
}
