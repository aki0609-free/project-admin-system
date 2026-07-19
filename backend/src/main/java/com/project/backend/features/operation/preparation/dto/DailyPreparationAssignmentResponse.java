package com.project.backend.features.operation.preparation.dto;

import lombok.Builder;

@Builder
public record DailyPreparationAssignmentResponse(
        Long id,
        Long preparationId,

        Long employeeId,
        String employeeCode,
        String employeeName,

        Long customerId,
        Long customerSiteId,
        String customerName,
        String siteName,

        String workDescription
) {
}