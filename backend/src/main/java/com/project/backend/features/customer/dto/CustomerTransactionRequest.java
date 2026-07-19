package com.project.backend.features.customer.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.features.customer.enums.CustomerPaymentStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerTransactionRequest(
        Long id,
        Long customerId,
        String targetMonth,
        DayRule closingDayRule,
        DayRule paymentDayRule,
        Integer billingAmount,
        LocalDate expectedPaymentDate,
        LocalDate confirmedPaymentDate,
        Integer paidAmount,
        Integer fee,
        Integer offsetAmount,
        Integer totalAmount,
        CustomerPaymentStatus paymentStatus,
        String note
) {
}