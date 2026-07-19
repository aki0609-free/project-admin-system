package com.project.backend.features.customer.dto;

public record CustomerSiteOptionItemResponse(
        Long id,
        Long customerId,
        String name,
        Integer distanceFromCompanyKm
) {
}