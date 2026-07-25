package com.project.backend.features.system.rule.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.entity.RuleParameter;
import com.project.backend.features.system.rule.enums.RuleDataType;
import com.project.backend.features.system.rule.service.converter.RuleValueConverter;

class RuleParameterResolverTest {

    private final RuleParameterResolver resolver =
            new RuleParameterResolver(new RuleValueConverter());

    @Test
    void resolve_shouldApplyDefaultAndConvertDeclaredParameters() {
        RuleMaster rule = ruleWithParameters(
                parameter(
                        "rate",
                        RuleDataType.DECIMAL,
                        true,
                        "1.25"
                ),
                parameter(
                        "count",
                        RuleDataType.INTEGER,
                        true,
                        null
                )
        );

        Map<String, Object> result = resolver.resolve(
                rule,
                Map.of(
                        "count",
                        "3",
                        "systemValue",
                        "preserved"
                )
        );

        assertThat(result.get("rate"))
                .isEqualTo(new BigDecimal("1.25"));
        assertThat(result.get("count")).isEqualTo(3);
        assertThat(result.get("systemValue"))
                .isEqualTo("preserved");
    }

    @Test
    void resolve_shouldRejectMissingRequiredParameter() {
        RuleMaster rule = ruleWithParameters(parameter(
                "employeeId",
                RuleDataType.LONG,
                true,
                null
        ));

        assertThatThrownBy(() ->
                resolver.resolve(rule, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employeeId");
    }

    private RuleMaster ruleWithParameters(
            RuleParameter... parameters
    ) {
        RuleMaster rule = new RuleMaster();
        rule.setParameters(List.of(parameters));
        return rule;
    }

    private RuleParameter parameter(
            String name,
            RuleDataType dataType,
            boolean required,
            String defaultValue
    ) {
        RuleParameter parameter = new RuleParameter();
        parameter.setParamName(name);
        parameter.setDataType(dataType);
        parameter.setRequiredFlag(required);
        parameter.setDefaultValue(defaultValue);
        parameter.setOrderNo(1);
        return parameter;
    }
}
