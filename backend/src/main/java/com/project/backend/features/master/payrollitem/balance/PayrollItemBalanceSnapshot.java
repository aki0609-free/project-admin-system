package com.project.backend.features.master.payrollitem.balance;

import java.math.BigDecimal;

public record PayrollItemBalanceSnapshot(
        boolean tracked,
        BalanceUnit unit,
        BigDecimal openingQuantity,
        BigDecimal accruedQuantity,
        BigDecimal consumedQuantity,
        BigDecimal remainingQuantity
) {
    public static PayrollItemBalanceSnapshot untracked() {
        return new PayrollItemBalanceSnapshot(
                false, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO
        );
    }
}
