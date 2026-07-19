package com.project.backend.features.master.deduction.dto;

import java.util.Map;

public record DeductionValueRequest(
        Long deductionMasterId,
        String deductionCode,
        Integer manualAmount,
        Map<String, Object> parameters
) {
}