package com.project.backend.features.employee.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public record EmployeePayrollItemSettingRequest(
        @NotNull PayrollItemTargetType targetType,
        @NotBlank String targetCode,
        boolean enabled,
        Map<String, String> parameters
) {
}
