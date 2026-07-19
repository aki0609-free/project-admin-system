package com.project.backend.features.operation.daily.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Builder;

@Builder
public record DailyPaymentPrintSummaryResponse(
        LocalDate paymentDate,
        Integer employeeCount,
        BigDecimal totalPlannedAmount,
        BigDecimal totalActualAmount,
        DailyPaymentDenominationResponse totalDenomination,
        List<DailyPaymentPrintDetailResponse> details
) {
}