package com.project.backend.features.employee.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.project.backend.features.employee.enums.EmployeeFinanceAccountType;
import com.project.backend.features.employee.enums.EmployeeFinanceTransactionType;

import lombok.Builder;

@Builder
public record EmployeeFinanceTransactionResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        EmployeeFinanceAccountType accountType,
        EmployeeFinanceTransactionType transactionType,
        Long accountReferenceId,
        Long dailyReportId,
        LocalDate transactionDate,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String note,
        Instant createdAt
) {
}
