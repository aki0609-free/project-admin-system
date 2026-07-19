package com.project.backend.features.customer.dto;

public record CustomerSiteResponse(
        Long id,
        Long customerId,
        String name,
        String contactPersonName,
        String contactPersonPhone,
        String contactPersonEmail,
        Integer distanceFromCompanyKm
) {
}