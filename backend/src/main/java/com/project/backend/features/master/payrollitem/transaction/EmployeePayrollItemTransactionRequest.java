package com.project.backend.features.master.payrollitem.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeePayrollItemTransactionRequest(
        @NotBlank String targetCode,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String targetMonth,
        @NotNull LocalDate transactionDate,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @DecimalMin(value = "0.00") BigDecimal quantity,
        @NotNull PayrollItemTransactionStatus status,
        @Size(max = 150) String sourceReference,
        @Size(max = 500) String note
) {
}
