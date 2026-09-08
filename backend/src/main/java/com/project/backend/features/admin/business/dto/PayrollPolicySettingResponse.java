package com.project.backend.features.admin.business.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

public record PayrollPolicySettingResponse(
        Long id,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        DayOfWeek weekStartDay,
        BigDecimal weeklyStatutoryHours,
        BigDecimal monthlyOvertimeThresholdHours,
        BigDecimal overtimeRate,
        BigDecimal overtimeOverThresholdRate,
        BigDecimal nightPremiumRate,
        BigDecimal statutoryHolidayRate,
        BigDecimal dailyStandardHours,
        RoundingMode amountRoundingMode,
        boolean activeFlag
) {
}
