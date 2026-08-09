package com.project.backend.features.employee.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public record EmployeePayrollItemSettingRequest(
        @NotBlank String targetCode,
        boolean enabled,
        Map<String, String> parameters
) {
}
