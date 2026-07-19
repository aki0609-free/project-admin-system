package com.project.backend.features.master.payrollitem.dto;

import java.util.Map;

import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public record PayrollItemValueRequest(
        PayrollItemTargetType targetType,
        Long targetMasterId,
        String targetCode,
        Integer manualAmount,
        Map<String, Object> parameters
) {
}