package com.project.backend.features.master.payrollitem.dto;

import java.util.Map;

import com.project.backend.features.master.payrollitem.enums.PayrollItemQueryType;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public record PayrollItemCalculationRequest(
        PayrollItemQueryType queryType,
        PayrollItemTargetType targetType,
        Map<String, Object> parameters
) {
}