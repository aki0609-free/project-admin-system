package com.project.backend.features.master.payrollitem.parameter;

import java.time.LocalDate;
import java.util.Map;

public record PayrollItemRuleParameterResolutionContext(
        Long employeeId,
        LocalDate targetDate,
        Map<String, String> employeeSettings
) {
}
