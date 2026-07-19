package com.project.backend.features.customer.dto;

public record CustomerEmployeeResponse(
        Long id,
        Long customerId,
        String name,
        String furiganaName,
        String position,
        String phone,
        String email,
        Boolean invoiceToFlag,
        Boolean invoiceCcFlag
) {
}