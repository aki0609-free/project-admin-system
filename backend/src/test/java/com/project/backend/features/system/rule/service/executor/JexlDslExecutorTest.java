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

    @Test
    void execute_shouldCalculateDriverAllowanceOnlyForEmployeeArrangement() {
        RuleMaster rule = new RuleMaster();
        rule.setRuleName("DAILY_DRIVER_ALLOWANCE");
        rule.setDslText("vehicleArrangementType == 'EMPLOYEE' ? "
                + "mileage * distanceUnitPrice + passengerCount * passengerUnitPrice : 0");

        Object employeeResult = executor.execute(
                RuleExecutionContext.builder()
                        .rule(rule)
                        .facts(Map.of(
                                "vehicleArrangementType", "EMPLOYEE",
                                "mileage", new BigDecimal("10"),
                                "distanceUnitPrice", new BigDecimal("15"),
                                "passengerCount", 2,
                                "passengerUnitPrice", new BigDecimal("200")
                        ))
                        .parameters(Map.of())
                        .build()
        );
        Object companyResult = executor.execute(
                RuleExecutionContext.builder()
                        .rule(rule)
                        .facts(Map.of(
                                "vehicleArrangementType", "COMPANY",
                                "mileage", new BigDecimal("10"),
                                "distanceUnitPrice", new BigDecimal("15"),
                                "passengerCount", 0,
                                "passengerUnitPrice", new BigDecimal("200")
                        ))
                        .parameters(Map.of())
                        .build()
        );

        assertThat(employeeResult).isEqualTo(new BigDecimal("550"));
        assertThat(companyResult).isEqualTo(0);
    }
}
