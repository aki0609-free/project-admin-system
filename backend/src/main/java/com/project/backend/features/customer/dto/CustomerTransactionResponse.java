package com.project.backend.features.customer.dto;

import java.time.LocalDate;

import com.project.backend.common.dayrule.dto.DayRuleResponse;

public record CustomerTransactionResponse(
        Long id,
        Long customerId,
        String targetMonth,
        DayRuleResponse closingDayRule,
        DayRuleResponse paymentDayRule,
        Integer billingAmount,
        LocalDate expectedPaymentDate,
        LocalDate confirmedPaymentDate,
        Integer paidAmount,
        Integer fee,
        Integer offsetAmount,
        Integer totalAmount,
        String paymentStatus,
        String note
) {
}