package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import com.project.backend.features.employee.enums.SalaryType;

import lombok.Builder;

@Builder
public record DailyReportMonthlyAttendanceResponse(
        Long employeeId,
        String employeeCode,
        String employeeName,

        YearMonth targetMonth,
        LocalDate periodStart,
        LocalDate periodEnd,

        SalaryType salaryType,
        BigDecimal baseSalaryAmount,
        BigDecimal grossSalaryAmount,

        Integer reportCount,

        BigDecimal paidLeaveUsedDays,
        BigDecimal paidLeaveRemainingDays,
        BigDecimal paidLeaveRemainingAfterUsedDays,

        BigDecimal totalWorkHours,
        BigDecimal totalOvertimeHours,
        BigDecimal totalNightWorkHours,

        BigDecimal totalAllowanceAmount,
        BigDecimal totalDeductionAmount,

        BigDecimal totalSavingAmount,
        BigDecimal totalLoanRepaymentAmount,

        BigDecimal estimatedPaymentAmount
) {
}