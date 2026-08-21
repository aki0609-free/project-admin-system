package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;

public record DailyReportAllowanceSaveRequest(
        Long allowanceMasterId,
        String allowanceCode,
        String allowanceName,
        Integer calculatedAmount,
        Integer amount,
        Boolean manualOverride,
        String overrideReason,
        BigDecimal quantity,
        String balanceUnit
) {
    public DailyReportAllowanceSaveRequest(
            Long allowanceMasterId,
            String allowanceCode,
            String allowanceName,
            Integer amount
    ) {
        this(allowanceMasterId, allowanceCode, allowanceName,
                amount, amount, false, null, null, null);
    }
}
