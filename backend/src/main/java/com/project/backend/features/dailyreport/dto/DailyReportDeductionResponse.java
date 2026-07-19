package com.project.backend.features.dailyreport.dto;

import lombok.Builder;

@Builder
public record DailyReportDeductionResponse(
        Long id,
        Long deductionMasterId,
        String deductionCode,
        String deductionName,
        Integer amount
) {
}