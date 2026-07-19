package com.project.backend.features.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.enums.SalaryType;

import lombok.Builder;

@Builder
public record EmployeeContractQueryResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        boolean renewalFlag,
        SalaryType salaryType,
        PaymentCycle paymentCycle,
        BigDecimal monthlySalary,
        BigDecimal weeklyWage,
        BigDecimal dailyWage,
        BigDecimal hourlyWage,
        BigDecimal standardWorkingHours,
        String note
) {
}