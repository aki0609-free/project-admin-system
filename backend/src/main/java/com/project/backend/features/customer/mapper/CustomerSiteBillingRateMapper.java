package com.project.backend.features.customer.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.project.backend.features.customer.dto.CustomerSiteBillingRateRequest;
import com.project.backend.features.customer.dto.CustomerSiteBillingRateResponse;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.entity.CustomerSiteBillingRate;
import com.project.backend.features.customer.enums.CustomerBillingUnit;

@Component
public class CustomerSiteBillingRateMapper {

    public CustomerSiteBillingRate toEntity(
            CustomerSite customerSite,
            CustomerSiteBillingRateRequest request
    ) {
        CustomerSiteBillingRate entity =
                new CustomerSiteBillingRate();

        entity.setCustomerSite(customerSite);

        apply(
                entity,
                request
        );

        return entity;
    }

    public void apply(
            CustomerSiteBillingRate entity,
            CustomerSiteBillingRateRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "現場請求単価リクエストは必須です。"
            );
        }

        entity.setJobCode(
                trimToNull(request.jobCode())
        );

        entity.setJobName(
                trimToNull(request.jobName())
        );

        entity.setSiteRoleCode(
                trimToNull(request.siteRoleCode()) == null
                        ? "GENERAL"
                        : request.siteRoleCode().trim()
        );

        entity.setSiteRoleName(
                trimToNull(request.siteRoleName()) == null
                        ? "一般"
                        : request.siteRoleName().trim()
        );

        entity.setBillingUnit(
                request.billingUnit() == null
                        ? CustomerBillingUnit.DAILY
                        : request.billingUnit()
        );

        entity.setBaseUnitPrice(
                toZero(request.baseUnitPrice())
        );

        entity.setOvertimeUnitPrice(
                toZero(request.overtimeUnitPrice())
        );

        entity.setNightUnitPrice(
                toZero(request.nightUnitPrice())
        );

        entity.setHolidayUnitPrice(
                toZero(request.holidayUnitPrice())
        );

        entity.setCommuteUnitPrice(
                toZero(request.commuteUnitPrice())
        );

        entity.setEffectiveFrom(
                request.effectiveFrom()
        );

        entity.setEffectiveTo(
                request.effectiveTo()
        );

        entity.setDisplayOrder(
                request.displayOrder() == null
                        ? 1
                        : request.displayOrder()
        );

        entity.setActiveFlag(
                request.activeFlag() == null
                        ? true
                        : request.activeFlag()
        );

        entity.setNote(
                trimToNull(request.note())
        );
    }

    public CustomerSiteBillingRateResponse toResponse(
            CustomerSiteBillingRate entity
    ) {
        return new CustomerSiteBillingRateResponse(
                entity.getId(),
                entity.getCustomerSite().getId(),

                entity.getJobCode(),
                entity.getJobName(),

                entity.getSiteRoleCode(),
                entity.getSiteRoleName(),

                entity.getBillingUnit(),

                entity.getBaseUnitPrice(),
                entity.getOvertimeUnitPrice(),
                entity.getNightUnitPrice(),
                entity.getHolidayUnitPrice(),
                entity.getCommuteUnitPrice(),

                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),

                entity.getDisplayOrder(),
                entity.getActiveFlag(),
                entity.getNote()
        );
    }

    private BigDecimal toZero(
            BigDecimal value
    ) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    private String trimToNull(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}