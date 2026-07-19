package com.project.backend.features.operation.daily.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.features.employee.enums.SalaryType;

import lombok.Builder;

@Builder
public record DailyPaymentSummaryResponse(
        LocalDate paymentDate,

        Long dailyReportId,
        Long employeeId,
        String employeeCode,
        String employeeName,

        SalaryType salaryType,

        BigDecimal basePayAmount,
        BigDecimal allowanceAmount,
        BigDecimal deductionAmount,
        BigDecimal savingAmount,
        BigDecimal loanRepaymentAmount,

        BigDecimal plannedAmount
) {
}