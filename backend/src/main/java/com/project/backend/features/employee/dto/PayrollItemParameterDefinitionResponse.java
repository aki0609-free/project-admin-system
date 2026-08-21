package com.project.backend.features.employee.dto;

import java.util.List;

public record PayrollItemParameterDefinitionResponse(
        String key,
        String displayName,
        String inputType,
        boolean required,
        String defaultValue,
        List<PayrollItemParameterOptionResponse> options,
        boolean ruleParameter,
        boolean dailyDisplay,
        boolean inputSourceOverride,
        String ruleValueResolverKey,
        int displayOrder
) {
    public PayrollItemParameterDefinitionResponse(
            String key,
            String displayName,
            String inputType,
            boolean required,
            String defaultValue,
            List<PayrollItemParameterOptionResponse> options,
            boolean ruleParameter,
            boolean dailyDisplay,
            boolean inputSourceOverride,
            int displayOrder
    ) {
        this(key, displayName, inputType, required, defaultValue, options,
                ruleParameter, dailyDisplay, inputSourceOverride, null,
                displayOrder);
    }
}
