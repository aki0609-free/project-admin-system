package com.project.backend.features.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.features.customer.enums.CustomerBillingUnit;

public record CustomerSiteBillingRateRequest(
        Long id,

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
        String note,

        Boolean _isNew,
        Boolean _isUpdated,
        Boolean _isDeleted
) {
}