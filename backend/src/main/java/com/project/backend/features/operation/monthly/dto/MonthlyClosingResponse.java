package com.project.backend.features.operation.monthly.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;

import lombok.Builder;

@Builder
public record MonthlyClosingResponse(
        Long id,
        LocalDate targetMonth,

        MonthlyClosingStatus status,

        Integer closingVersion,

        LocalDate closingStartDate,
        LocalDate closingEndDate,

        String closingRuleType,
        Integer closingRuleValue,

        Instant closedAt,

        String closedBy,

        String note
) {
}