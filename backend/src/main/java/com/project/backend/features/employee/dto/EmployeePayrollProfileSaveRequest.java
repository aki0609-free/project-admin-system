package com.project.backend.features.employee.dto;

import java.math.BigDecimal;

import com.project.backend.features.employee.enums.TaxCategory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EmployeePayrollProfileSaveRequest(
        @NotNull TaxCategory taxCategory,
        @Min(0) Integer taxDependentCount,
        Boolean dependentFlag,
        Boolean dependentOfOtherFlag,
        @DecimalMin("0.0") BigDecimal paidLeaveRemainingDays,
        Boolean incomeTaxCalcFlag,
        Boolean residentTaxCalcFlag,
        @DecimalMin("0.0") BigDecimal residentTaxMonthly,
        Boolean employmentInsuranceFlag,
        Boolean socialInsuranceFlag,
        Boolean healthInsuranceFlag,
        Boolean pensionInsuranceFlag,
        Boolean careInsuranceFlag,
        @DecimalMin("0.0") BigDecimal commuteAllowanceMonthly
) {
}
