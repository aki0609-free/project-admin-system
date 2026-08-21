package com.project.backend.features.master.payrollitem.dto;

import java.util.Map;
import java.util.Set;

import com.project.backend.features.master.payrollitem.enums.PayrollItemQueryType;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public record PayrollItemCalculationRequest(
        PayrollItemQueryType queryType,
        PayrollItemTargetType targetType,
        Map<String, Object> parameters,
        Map<Long, Map<String, Object>> itemParameters,
        Set<Long> excludedMasterIds
) {
    public PayrollItemCalculationRequest(
            PayrollItemQueryType queryType,
            PayrollItemTargetType targetType,
            Map<String, Object> parameters
    ) {
        this(queryType, targetType, parameters, Map.of(), Set.of());
    }

    public PayrollItemCalculationRequest(
            PayrollItemQueryType queryType,
            PayrollItemTargetType targetType,
            Map<String, Object> parameters,
            Map<Long, Map<String, Object>> itemParameters
    ) {
        this(queryType, targetType, parameters, itemParameters, Set.of());
    }
}
