package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record DailyReportEstimatedPayPreviewResponse(
        BigDecimal estimatedBasePayAmount,
        BigDecimal estimatedGrossPayAmount,
        BigDecimal estimatedNetPayAmount
) {
}