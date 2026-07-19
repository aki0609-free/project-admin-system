package com.project.backend.features.operation.daily.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.project.backend.features.operation.daily.enums.DailyPaymentStatus;

import lombok.Builder;

@Builder
public record DailyPaymentResponse(
        Long id,
        LocalDate paymentDate,
        Long employeeId,
        String employeeCode,
        String employeeName,

        BigDecimal plannedAmount,
        BigDecimal actualAmount,

        DailyPaymentStatus status,
        Instant paidAt,
        String note
) {
}