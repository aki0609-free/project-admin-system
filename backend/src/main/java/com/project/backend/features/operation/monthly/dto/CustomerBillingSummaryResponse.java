package com.project.backend.features.operation.monthly.dto;

import java.util.List;

public record CustomerBillingSummaryResponse(
        String targetMonth,
        String status,
        int targetCount,
        int eligibleCount,
        int closedCount,
        List<CustomerBillingTargetResponse> customers
) {
}
