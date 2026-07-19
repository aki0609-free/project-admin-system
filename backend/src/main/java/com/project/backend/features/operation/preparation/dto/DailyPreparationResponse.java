package com.project.backend.features.operation.preparation.dto;

import java.time.LocalDate;
import java.util.List;

import com.project.backend.features.operation.preparation.enums.DailyPreparationStatus;

import lombok.Builder;

@Builder
public record DailyPreparationResponse(
        Long id,
        LocalDate targetDate,
        DailyPreparationStatus status,
        String note,
        List<DailyPreparationAssignmentResponse> assignments,
        List<DailyPreparationDispatchResponse> dispatches
) {
}