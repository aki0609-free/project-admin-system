package com.project.backend.features.operation.monthly.dto;

import java.time.LocalDate;

import com.project.backend.common.dayrule.dto.DayRule;

/**
 * 顧客別の請求対象期間。
 *
 * 社内の給与締め期間とは独立して扱う。
 */
public record CustomerBillingPeriod(
        String targetMonth,
        LocalDate startDate,
        LocalDate endDate,
        DayRule rule
) {
}
