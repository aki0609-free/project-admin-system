package com.project.backend.features.customer.dto;

import java.util.List;

public record CustomerSiteBillingRateBulkSaveRequest(
        List<CustomerSiteBillingRateRequest> created,
        List<CustomerSiteBillingRateRequest> updated,
        List<Long> deletedIds
) {
}
