package com.project.backend.features.master.allowance.dto;

import java.util.Map;

public record AllowanceValueRequest(
        Long allowanceMasterId,
        String allowanceCode,
        Integer manualAmount,
        Map<String, Object> parameters
) {
}