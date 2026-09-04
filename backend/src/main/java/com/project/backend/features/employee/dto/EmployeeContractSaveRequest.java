package com.project.backend.features.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.enums.SalaryType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployeeContractSaveRequest(
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        @NotNull SalaryType salaryType,
        @NotNull PaymentCycle paymentCycle,
        @DecimalMin("0.0") BigDecimal monthlySalary,
        @DecimalMin("0.0") BigDecimal weeklyWage,
        @DecimalMin("0.0") BigDecimal dailyWage,
        @DecimalMin("0.0") BigDecimal hourlyWage,
        @DecimalMin("0.0") BigDecimal standardWorkingHours,
        @Size(max = 1000) String note
) {
}
