package com.project.backend.features.employee.dto;

import java.math.BigDecimal;

public record EmployeeFinanceSummaryResponse(
        Long employeeId,
        BigDecimal loanBalance,
        BigDecimal savingBalance,
        BigDecimal monthlyLoanRepayment,
        BigDecimal monthlySavingAmount
) {
}