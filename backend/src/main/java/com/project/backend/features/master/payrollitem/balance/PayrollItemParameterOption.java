package com.project.backend.features.master.payrollitem.balance;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

public record PayrollItemParameterOption(
        @NotBlank String label,
        @NotBlank String value,
        BigDecimal calculationValue
) {
    public PayrollItemParameterOption(String label, String value) {
        this(label, value, null);
    }
}
