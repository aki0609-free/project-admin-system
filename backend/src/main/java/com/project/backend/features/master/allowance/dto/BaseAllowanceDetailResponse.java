package com.project.backend.features.master.allowance.dto;

import java.util.Map;

public record BaseAllowanceDetailResponse(
        Long id,
        String detailType,
        String label,
        Map<String, Object> values
) {
}