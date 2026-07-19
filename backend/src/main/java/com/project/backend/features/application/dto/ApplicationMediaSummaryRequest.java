package com.project.backend.features.application.dto;

import java.util.List;
import java.util.Map;

public record ApplicationMediaSummaryRequest(
        Integer totalCost,
        Integer totalHires,
        Integer averageUnitPrice,
        List<Map<String, Object>> mediaSummary,
        List<Map<String, Object>> monthlySummary
) {
}