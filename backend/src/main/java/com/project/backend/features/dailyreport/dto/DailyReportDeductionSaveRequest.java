package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;

public record DailyReportDeductionSaveRequest(
        Long deductionMasterId,
        String deductionCode,
        String deductionName,
        Integer calculatedAmount,
        Integer amount,
        Boolean manualOverride,
        String overrideReason,
        BigDecimal quantity,
        String balanceUnit
) {
    public DailyReportDeductionSaveRequest(
            Long deductionMasterId,
            String deductionCode,
            String deductionName,
            Integer amount
    ) {
        this(
                deductionMasterId, deductionCode, deductionName,
                amount, amount, false, null, null, null
        );
    }
}
