package com.project.backend.features.system.rule.service.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.rule.context.RuleExecutionContext;
import com.project.backend.features.system.rule.entity.RuleMaster;

class JexlDslExecutorTest {

    private final JexlDslExecutor executor =
            new JexlDslExecutor();

    @Test
    void execute_shouldCalculateUsingFacts() {
        RuleMaster rule = new RuleMaster();
        rule.setRuleName("TEST");
        rule.setDslText("hours * rate");

        Object result = executor.execute(
                RuleExecutionContext.builder()
                        .rule(rule)
                        .facts(Map.of(
                                "hours",
                                2,
                                "rate",
                                new BigDecimal("1500")
                        ))
                        .parameters(Map.of())
                        .build()
        );

        assertThat(result)
                .isEqualTo(new BigDecimal("3000"));
    }

    @Test
    void execute_shouldRejectUndefinedVariableInStrictMode() {
        RuleMaster rule = new RuleMaster();
        rule.setRuleName("TEST");
        rule.setDslText("missingValue + 1");

        assertThatThrownBy(() ->
                executor.execute(
                        RuleExecutionContext.builder()
                                .rule(rule)
                                .facts(Map.of())
                                .parameters(Map.of())
                                .build()
                ))
                .isInstanceOf(RuntimeException.class);
    }
}
