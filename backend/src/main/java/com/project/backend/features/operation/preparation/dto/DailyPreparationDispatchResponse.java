package com.project.backend.features.operation.preparation.dto;

import lombok.Builder;

@Builder
public record DailyPreparationDispatchResponse(
        Long id,
        Long preparationId,

        Long customerId,
        Long customerSiteId,
        String customerName,
        String siteName,

        Integer distanceFromCompanyKm,
        Integer vehicleCount,

        String note
) {
}