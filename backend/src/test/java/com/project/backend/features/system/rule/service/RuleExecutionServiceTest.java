package com.project.backend.features.system.rule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.system.rule.dto.RuleContextRequest;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.exception.RuleExecutionException;
import com.project.backend.features.system.rule.exception.RuleExecutionStage;
import com.project.backend.features.system.rule.service.builder.RuleFactBuilder;
import com.project.backend.features.system.rule.service.executor.DslExecutorDispatcher;
import com.project.backend.features.system.rule.service.loader.RuleLoader;
import com.project.backend.features.system.rule.service.validation.RuleParameterResolver;

class RuleExecutionServiceTest {

    private RuleLoader ruleLoader;
    private RuleFactBuilder factBuilder;
    private DslExecutorDispatcher dispatcher;
    private RuleParameterResolver parameterResolver;
    private RuleExecutionService service;
    private RuleMaster rule;

    @BeforeEach
    void setUp() {
        ruleLoader = mock(RuleLoader.class);
        factBuilder = mock(RuleFactBuilder.class);
        dispatcher = mock(DslExecutorDispatcher.class);
        parameterResolver = mock(RuleParameterResolver.class);
        service = new RuleExecutionService(
                ruleLoader,
                factBuilder,
                dispatcher,
                parameterResolver
        );
        rule = new RuleMaster();
        rule.setRuleName("DAILY_PAY");
        rule.setResultFactKey("result");
        when(ruleLoader.loadActive("DAILY_PAY")).thenReturn(rule);
    }

    @Test
    void execute_shouldKeepFactsAvailableOnSuccessfulAdminTestExecution() {
        Map<String, Object> parameters = Map.of("hours", 8);
        Map<String, Object> facts = new LinkedHashMap<>(parameters);
        when(parameterResolver.resolve(eq(rule), any())).thenReturn(parameters);
        when(factBuilder.build(eq(rule), any())).thenReturn(facts);
        when(dispatcher.execute(any())).thenReturn(new BigDecimal("12000"));

        var result = service.execute(
                "DAILY_PAY",
                RuleContextRequest.builder()
                        .parameters(parameters)
                        .build()
        );

        assertThat(result.result()).isEqualTo(new BigDecimal("12000"));
        assertThat(result.facts())
                .containsEntry("hours", 8)
                .containsEntry("result", new BigDecimal("12000"));
    }

    @Test
    void execute_shouldNotExposeDslFailureMessageOrCause() {
        String secret = "employeeEmail=secret@example.com";
        Map<String, Object> parameters = Map.of("employeeEmail", "secret@example.com");
        when(parameterResolver.resolve(eq(rule), any())).thenReturn(parameters);
        when(factBuilder.build(eq(rule), any()))
                .thenReturn(new LinkedHashMap<>(parameters));
        when(dispatcher.execute(any()))
                .thenThrow(new RuntimeException(secret));

        Throwable thrown = catchThrowable(() -> service.execute(
                        "DAILY_PAY",
                        RuleContextRequest.builder()
                                .parameters(parameters)
                                .build()
        ));
        assertThat(thrown).isInstanceOf(RuleExecutionException.class);
        RuleExecutionException exception = (RuleExecutionException) thrown;

        assertThat(exception.getStage()).isEqualTo(RuleExecutionStage.DSL_EXECUTION);
        assertThat(exception.getFailureType()).isEqualTo("RuntimeException");
        assertThat(exception.getMessage()).doesNotContain(secret, "secret@example.com");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void execute_shouldSanitizeParameterResolutionFailure() {
        String secret = "value=1234567890123456";
        when(parameterResolver.resolve(eq(rule), any()))
                .thenThrow(new IllegalArgumentException(secret));

        Throwable thrown = catchThrowable(() -> service.execute(
                        "DAILY_PAY",
                        RuleContextRequest.builder()
                                .parameters(Map.of("account", "1234567890123456"))
                                .build()
        ));
        assertThat(thrown).isInstanceOf(RuleExecutionException.class);
        RuleExecutionException exception = (RuleExecutionException) thrown;

        assertThat(exception.getStage())
                .isEqualTo(RuleExecutionStage.PARAMETER_RESOLUTION);
        assertThat(exception.getMessage()).doesNotContain(secret, "1234567890123456");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void execute_shouldNotRetainInvalidRequestedRuleName() {
        String secretRuleName = "secret@example.com\nvalue=1234";
        when(ruleLoader.loadActive(secretRuleName))
                .thenThrow(new RuntimeException("not found: " + secretRuleName));

        Throwable thrown = catchThrowable(
                () -> service.execute(secretRuleName, null)
        );
        assertThat(thrown).isInstanceOf(RuleExecutionException.class);
        RuleExecutionException exception = (RuleExecutionException) thrown;

        assertThat(exception.getStage()).isEqualTo(RuleExecutionStage.RULE_LOADING);
        assertThat(exception.getRuleName()).isEqualTo("<invalid>");
        assertThat(exception.getMessage()).doesNotContain("secret@example.com", "1234");
        assertThat(exception.getCause()).isNull();
    }
}
