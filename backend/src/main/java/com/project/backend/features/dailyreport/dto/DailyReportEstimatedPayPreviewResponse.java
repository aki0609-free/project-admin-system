package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record DailyReportEstimatedPayPreviewResponse(
        BigDecimal estimatedBasePayAmount,
        BigDecimal normalPayAmount,
        BigDecimal overtimePayAmount,
        BigDecimal nightPayAmount,
        BigDecimal holidayPayAmount,
        BigDecimal estimatedGrossPayAmount,
        BigDecimal estimatedNetPayAmount
) {
}
