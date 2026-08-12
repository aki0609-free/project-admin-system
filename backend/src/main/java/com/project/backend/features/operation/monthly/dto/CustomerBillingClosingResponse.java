package com.project.backend.features.operation.monthly.dto;

import java.time.Instant;
import java.time.LocalDate;

public record CustomerBillingClosingResponse(
        Long id,
        LocalDate targetMonth,
        Long customerId,
        String status,
        Integer closingVersion,
        Instant closedAt
) {
}
