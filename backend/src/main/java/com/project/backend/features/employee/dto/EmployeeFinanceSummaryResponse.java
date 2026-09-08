package com.project.backend.features.employee.dto;

import java.math.BigDecimal;

public record EmployeeFinanceSummaryResponse(
        Long employeeId,
        boolean hasActiveLoan,
        boolean hasActiveSaving,
        BigDecimal loanBalance,
        BigDecimal savingBalance,
        BigDecimal monthlyLoanRepayment,
        BigDecimal monthlySavingAmount
) {
}
