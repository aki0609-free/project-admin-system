package com.project.backend.features.customer.dto;

import java.math.BigDecimal;

public record CustomerSiteResponse(
        Long id,
        Long customerId,
        String name,
        String contactPersonName,
        String contactPersonPhone,
        String contactPersonEmail,
        BigDecimal distanceFromCompanyKm
) {
}
