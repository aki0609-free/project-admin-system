package com.project.backend.features.master.payrollitem.dto;

import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public record PayrollItemMasterSnapshot(
        PayrollItemTargetType targetType,
        Long id,
        String code,
        String name,
        String calculationType,
        String ruleName,
        Integer defaultAmount,
        Integer minAmount,
        Integer maxAmount,
        Boolean allowManualInput,
        Integer displayOrder
) {
}