package com.project.backend.features.operation.daily.dto;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record DailyPaymentPrintDetailResponse(
        Long employeeId,
        String employeeCode,
        String employeeName,
        BigDecimal plannedAmount,
        BigDecimal actualAmount,
        String note,
        DailyPaymentDenominationResponse denomination
) {
}