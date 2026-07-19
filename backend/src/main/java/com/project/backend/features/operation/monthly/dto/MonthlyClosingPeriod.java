package com.project.backend.features.operation.monthly.dto;

import java.time.LocalDate;

import com.project.backend.common.dayrule.dto.DayRule;

public record MonthlyClosingPeriod(
        String targetMonth,
        LocalDate startDate,
        LocalDate endDate,
        DayRule rule
) {
}