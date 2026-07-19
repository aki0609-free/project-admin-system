package com.project.backend.features.operation.monthly.utils;

import java.time.YearMonth;

public final class MonthlyOperationDateUtil {

    private MonthlyOperationDateUtil() {
    }

    public static YearMonth parseTargetMonth(String targetMonthText) {
        if (targetMonthText == null || targetMonthText.isBlank()) {
            throw new RuntimeException("targetMonth は必須です。");
        }

        return YearMonth.parse(targetMonthText);
    }
}