package com.project.backend.features.admin.business.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PayrollPolicySettingSaveRequest(
        Long id,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @NotNull DayOfWeek weekStartDay,
        @NotNull @DecimalMin("0.01") BigDecimal weeklyStatutoryHours,
        @NotNull @DecimalMin("0.00") BigDecimal monthlyOvertimeThresholdHours,
        @NotNull @DecimalMin("0.00") BigDecimal overtimeRate,
        @NotNull @DecimalMin("0.00") BigDecimal overtimeOverThresholdRate,
        @NotNull @DecimalMin("0.00") BigDecimal nightPremiumRate,
        @NotNull @DecimalMin("0.00") BigDecimal statutoryHolidayRate,
        @NotNull @DecimalMin("0.01") BigDecimal dailyStandardHours,
        @NotNull RoundingMode amountRoundingMode,
        @NotNull Boolean activeFlag
) {
}
