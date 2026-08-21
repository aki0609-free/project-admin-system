package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record DailyReportAllowanceResponse(
        Long id,
        Long allowanceMasterId,
        String allowanceCode,
        String allowanceName,
        Integer calculatedAmount,
        Integer amount,
        Boolean manualOverride,
        String overrideReason,
        BigDecimal quantity,
        String balanceUnit
) {
}
