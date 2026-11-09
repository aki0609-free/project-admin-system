package com.project.backend.features.operation.preparation.dto;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public record DailyPreparationDispatchResponse(
        Long id,
        Long preparationId,

        Long customerId,
        Long customerSiteId,
        String customerName,
        String siteName,

        BigDecimal distanceFromCompanyKm,
        Integer vehicleCount,
        BigDecimal otherAmount,

        String note
) {
}
