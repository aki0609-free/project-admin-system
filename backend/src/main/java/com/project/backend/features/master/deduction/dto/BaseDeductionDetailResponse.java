package com.project.backend.features.master.deduction.dto;

import java.util.Map;

public record BaseDeductionDetailResponse(
        Long id,
        String detailType,
        String label,
        Map<String, Object> values
) {
}