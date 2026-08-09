package com.project.backend.features.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record EmployeePayrollItemSettingResponse(
        String targetCode,
        String displayName,
        boolean enabled,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String balanceUnit,
        BigDecimal openingQuantity,
        BigDecimal accruedQuantity,
        BigDecimal consumedQuantity,
        BigDecimal remainingQuantity,
        Map<String, String> parameters
) {
}
