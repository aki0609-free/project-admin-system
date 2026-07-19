package com.project.backend.features.customer.dto;

import java.time.LocalDate;

public record CustomerPaymentConfirmRequest(
        LocalDate confirmedPaymentDate,
        Integer paidAmount,
        Integer fee,
        Integer offsetAmount,
        String note
) {
}