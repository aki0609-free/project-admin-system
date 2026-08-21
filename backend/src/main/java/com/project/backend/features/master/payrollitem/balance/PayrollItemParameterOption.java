package com.project.backend.features.master.payrollitem.balance;

import jakarta.validation.constraints.NotBlank;

public record PayrollItemParameterOption(
        @NotBlank String label,
        @NotBlank String value
) {
}
