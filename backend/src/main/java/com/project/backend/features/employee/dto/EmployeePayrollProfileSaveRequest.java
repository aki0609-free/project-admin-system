package com.project.backend.features.employee.dto;

import java.math.BigDecimal;

import com.project.backend.features.employee.enums.TaxCategory;

public record EmployeePayrollProfileSaveRequest(
        TaxCategory taxCategory,
        Integer taxDependentCount,
        Boolean dependentFlag,
        Boolean dependentOfOtherFlag,
        BigDecimal paidLeaveRemainingDays,
        Boolean incomeTaxCalcFlag,
        Boolean residentTaxCalcFlag,
        BigDecimal residentTaxMonthly,
        Boolean employmentInsuranceFlag,
        Boolean socialInsuranceFlag,
        Boolean healthInsuranceFlag,
        Boolean pensionInsuranceFlag,
        Boolean careInsuranceFlag,
        Boolean dailyPayFlag,
        BigDecimal commuteAllowanceMonthly
) {
}