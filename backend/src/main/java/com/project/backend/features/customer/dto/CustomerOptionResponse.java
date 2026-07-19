package com.project.backend.features.customer.dto;

import java.util.List;

public record CustomerOptionResponse(
        List<CustomerOptionItemResponse> customers,
        List<CustomerSiteOptionItemResponse> sites
) {
}