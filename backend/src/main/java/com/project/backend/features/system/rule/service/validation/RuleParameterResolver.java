package com.project.backend.features.system.rule.service.validation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.entity.RuleParameter;
import com.project.backend.features.system.rule.service.converter.RuleValueConverter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RuleParameterResolver {

    private final RuleValueConverter valueConverter;

    public Map<String, Object> resolve(
            RuleMaster rule,
            Map<String, Object> requestedParameters
    ) {
        Map<String, Object> input = requestedParameters != null
                ? requestedParameters
                : Map.of();
        Map<String, Object> resolved = new LinkedHashMap<>(input);

        if (rule.getParameters() == null) {
            return resolved;
        }

        rule.getParameters().stream()
                .filter(parameter -> parameter.getDeletedAt() == null)
                .sorted(Comparator.comparingInt(
                        RuleParameter::getOrderNo
                ))
                .forEach(parameter ->
                        applyParameter(resolved, input, parameter));

        return resolved;
    }

    private void applyParameter(
            Map<String, Object> resolved,
            Map<String, Object> input,
            RuleParameter parameter
    ) {
        String name = parameter.getParamName();
        Object value = input.get(name);

        if (isMissing(value)
                && StringUtils.hasText(parameter.getDefaultValue())) {
            value = parameter.getDefaultValue();
        }

        if (isMissing(value)) {
            if (parameter.isRequiredFlag()) {
                throw new IllegalArgumentException(
                        "必須Ruleパラメータが未設定です。 paramName="
                                + name
                );
            }

            resolved.remove(name);
            return;
        }

        resolved.put(
                name,
                valueConverter.convert(
                        value,
                        parameter.getDataType(),
                        "Ruleパラメータ " + name
                )
        );
    }

    private boolean isMissing(Object value) {
        return value == null
                || value instanceof String text
                && !StringUtils.hasText(text);
    }
}
