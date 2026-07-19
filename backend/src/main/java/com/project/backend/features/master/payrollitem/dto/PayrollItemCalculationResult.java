package com.project.backend.features.master.payrollitem.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public record PayrollItemCalculationResult(
        PayrollItemTargetType targetType,
        Long targetMasterId,
        String targetCode,
        String targetName,
        String calculationType,
        String ruleName,
        BigDecimal amount,
        Boolean allowManualInput,
        Integer displayOrder,
        Map<String, Object> facts
) {
}