package com.project.backend.features.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.enums.SalaryType;

public record EmployeeContractSaveRequest(
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        Boolean renewalFlag,
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