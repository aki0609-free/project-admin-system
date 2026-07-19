package com.project.backend.features.customer.dto;

import java.time.LocalDate;

import com.project.backend.common.dayrule.dto.DayRule;

public record CustomerTransactionClosingRequest(
        Long customerId,
        String targetMonth,
        DayRule closingDayRule,
        DayRule paymentDayRule,
        Integer billingAmount,
        LocalDate expectedPaymentDate,
        String note
) {
}