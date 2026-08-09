package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record DailyReportDeductionResponse(
        Long id,
        Long deductionMasterId,
        String deductionCode,
        String deductionName,
        Integer calculatedAmount,
        Integer amount,
        Boolean manualOverride,
        String overrideReason,
        BigDecimal quantity,
        String balanceUnit
) {
}
