package com.project.backend.features.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.features.customer.enums.CustomerBillingUnit;

public record CustomerSiteBillingRateResponse(
        Long id,
        Long customerSiteId,

        String jobCode,
        String jobName,

        String siteRoleCode,
        String siteRoleName,

        CustomerBillingUnit billingUnit,

        BigDecimal baseUnitPrice,
        BigDecimal overtimeUnitPrice,
        BigDecimal nightUnitPrice,
        BigDecimal holidayUnitPrice,
        BigDecimal commuteUnitPrice,

        LocalDate effectiveFrom,
        LocalDate effectiveTo,

        Integer displayOrder,
        Boolean activeFlag,
        String note
) {
}