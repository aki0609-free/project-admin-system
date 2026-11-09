package com.project.backend.features.customer.dto;

import java.math.BigDecimal;

public record CustomerSiteOptionItemResponse(
        Long id,
        Long customerId,
        String name,
        BigDecimal distanceFromCompanyKm
) {
}
