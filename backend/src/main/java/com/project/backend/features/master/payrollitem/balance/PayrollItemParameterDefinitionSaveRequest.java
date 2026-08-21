package com.project.backend.features.master.payrollitem.balance;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayrollItemParameterDefinitionSaveRequest(
        @NotBlank String key,
        @NotBlank String displayName,
        @NotNull PayrollItemParameterInputType inputType,
        boolean required,
        String defaultValue,
        List<PayrollItemParameterOption> options,
        boolean ruleParameter,
        boolean dailyDisplay,
        boolean inputSourceOverride,
        String ruleValueResolverKey,
        int displayOrder
) {
    public PayrollItemParameterDefinitionSaveRequest(
            String key,
            String displayName,
            PayrollItemParameterInputType inputType,
            boolean required,
            String defaultValue,
            List<PayrollItemParameterOption> options,
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
