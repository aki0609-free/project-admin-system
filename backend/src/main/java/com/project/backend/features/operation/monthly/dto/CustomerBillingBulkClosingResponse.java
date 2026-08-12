package com.project.backend.features.operation.monthly.dto;

import java.util.List;

public record CustomerBillingBulkClosingResponse(
        int targetCount,
        int completedCount,
        int skippedBeforeClosingDateCount,
        int alreadyClosedCount,
        int failedCount,
        List<String> errors
) {
}
