package com.project.backend.features.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;

public record EmployeePayrollItemSettingResponse(
        String targetType,
        String targetCode,
        String displayName,
        boolean enabled,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String inputSource,
        boolean balanceTracked,
        String balanceUnit,
        BigDecimal openingQuantity,
        BigDecimal accruedQuantity,
        BigDecimal consumedQuantity,
        BigDecimal remainingQuantity,
        Map<String, String> parameters,
        List<PayrollItemParameterDefinitionResponse> parameterDefinitions
) {
}
