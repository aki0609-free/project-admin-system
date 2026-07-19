package com.project.backend.features.employee.dto;

import java.math.BigDecimal;

import com.project.backend.features.employee.enums.TaxCategory;

import lombok.Builder;

@Builder
public record EmployeePayrollProfileResponse(
        Long id,
        TaxCategory taxCategory,
        int taxDependentCount,
        boolean dependentFlag,
        boolean dependentOfOtherFlag,
        BigDecimal paidLeaveRemainingDays,
        boolean incomeTaxCalcFlag,
        boolean residentTaxCalcFlag,
        BigDecimal residentTaxMonthly,
        boolean employmentInsuranceFlag,
        boolean socialInsuranceFlag,
        boolean healthInsuranceFlag,
        boolean pensionInsuranceFlag,
        boolean careInsuranceFlag,
        boolean dailyPayFlag,
        BigDecimal commuteAllowanceMonthly
) {
}