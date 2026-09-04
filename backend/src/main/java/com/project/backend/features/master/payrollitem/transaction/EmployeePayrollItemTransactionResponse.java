package com.project.backend.features.master.payrollitem.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

public record EmployeePayrollItemTransactionResponse(
        Long id,
        Long employeeId,
        PayrollItemTargetType targetType,
        String targetCode,
        String targetName,
        String targetMonth,
        LocalDate transactionDate,
        BigDecimal amount,
        BigDecimal quantity,
        PayrollItemTransactionPurpose transactionPurpose,
        PayrollItemBalanceEffect balanceEffect,
        PayrollItemTransactionSource sourceType,
        String sourceReference,
        PayrollItemTransactionStatus status,
        String note,
        long lockVersion
) {
}
