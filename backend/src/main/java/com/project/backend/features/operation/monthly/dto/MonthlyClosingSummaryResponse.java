package com.project.backend.features.operation.monthly.dto;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record MonthlyClosingSummaryResponse(
        String targetMonth,
        Integer employeeCount,
        Integer workReportCount,
        BigDecimal totalGrossAmount,
        BigDecimal totalDeductionAmount,
        BigDecimal totalDailyPaymentAmount,
        BigDecimal totalNetPaymentAmount,
        MonthlyClosingResponse closing
) {
}