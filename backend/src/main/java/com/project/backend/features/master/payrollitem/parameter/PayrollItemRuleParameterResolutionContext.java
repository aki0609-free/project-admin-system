package com.project.backend.features.master.payrollitem.parameter;

import java.time.LocalDate;
import java.util.Map;

public record PayrollItemRuleParameterResolutionContext(
        Long employeeId,
        LocalDate targetDate,
        Map<String, String> employeeSettings,
        Long balancePolicyId,
        String resolverArgument
) {
    public PayrollItemRuleParameterResolutionContext(
            Long employeeId,
            LocalDate targetDate,
            Map<String, String> employeeSettings
    ) {
        this(employeeId, targetDate, employeeSettings, null, null);
    }

    public PayrollItemRuleParameterResolutionContext withResolverArgument(
            String argument
    ) {
        return new PayrollItemRuleParameterResolutionContext(
                employeeId, targetDate, employeeSettings,
                balancePolicyId, argument
        );
    }
}
