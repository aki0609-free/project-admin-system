package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;

public record DailyPayComponentAmounts(
        BigDecimal normalPayAmount,
        BigDecimal overtimePayAmount,
        BigDecimal nightPayAmount,
        BigDecimal holidayPayAmount
) {
    public BigDecimal total() {
        return normalPayAmount
                .add(overtimePayAmount)
                .add(nightPayAmount)
                .add(holidayPayAmount);
    }
}
