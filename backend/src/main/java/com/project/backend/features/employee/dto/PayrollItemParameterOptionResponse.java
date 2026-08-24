package com.project.backend.features.employee.dto;

import java.math.BigDecimal;

public record PayrollItemParameterOptionResponse(
        String label,
        String value,
        BigDecimal calculationValue
) {
    public PayrollItemParameterOptionResponse(String label, String value) {
        this(label, value, null);
    }
}
