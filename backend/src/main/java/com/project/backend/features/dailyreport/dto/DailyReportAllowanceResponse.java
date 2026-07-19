package com.project.backend.features.dailyreport.dto;

import lombok.Builder;

@Builder
public record DailyReportAllowanceResponse(
        Long id,
        Long allowanceMasterId,
        String allowanceCode,
        String allowanceName,
        Integer amount
) {
}