package com.project.backend.features.master.payrollitem.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeePayrollItemTransactionResponse(
        Long id,
        Long employeeId,
        String targetCode,
        String targetName,
        String targetMonth,
        LocalDate transactionDate,
        BigDecimal amount,
        BigDecimal quantity,
        PayrollItemTransactionSource sourceType,
        String sourceReference,
        PayrollItemTransactionStatus status,
        String note,
        long lockVersion
) {
}
