package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.project.backend.features.customer.entity.CustomerSiteBillingRate;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.service.CustomerSiteBillingRateQueryService;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.enums.VehicleArrangementType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyReportBillingRateService {

    private static final String DEFAULT_SITE_ROLE_CODE = "GENERAL";
    private static final String DEFAULT_SITE_ROLE_NAME = "一般";

    private final CustomerSiteBillingRateQueryService billingRateQueryService;
    private final CustomerRepository customerRepository;

    public void applyBillingRate(DailyReport dailyReport) {
        if (dailyReport == null) {
            throw new IllegalArgumentException(
                    "DailyReport は必須です。"
            );
        }

        /*
         * 現場未指定の日報は請求単価対象外として扱う。
         */
        if (dailyReport.getCustomerSiteId() == null) {
            clearBillingRate(dailyReport);
            return;
        }

        /*
         * 現場はあるが職種がない場合、単価を特定できない。
         */
        if (dailyReport.getJobCode() == null
                || dailyReport.getJobCode().isBlank()) {
            throw new IllegalArgumentException(
                    "現場を指定した場合、職種コードは必須です。"
            );
        }

        String siteRoleCode = normalizeSiteRoleCode(
                dailyReport.getSiteRoleCode()
        );

        CustomerSiteBillingRate rate =
                billingRateQueryService.findApplicableRate(
                        dailyReport.getCustomerSiteId(),
                        dailyReport.getJobCode().trim(),
                        siteRoleCode,
                        dailyReport.getWorkDate()
                );

        /*
         * 単価マスタの値を日報へスナップショット保存する。
         */
        dailyReport.setBillingRateId(
                rate.getId()
        );

        dailyReport.setJobCode(
                rate.getJobCode()
        );

        dailyReport.setJobName(
                rate.getJobName()
        );

        dailyReport.setSiteRoleCode(
                rate.getSiteRoleCode()
        );

        dailyReport.setSiteRoleName(
                rate.getSiteRoleName()
        );

        dailyReport.setBillingUnit(
                rate.getBillingUnit()
        );

        dailyReport.setBillingBaseUnitPrice(
                nvl(rate.getBaseUnitPrice())
        );

        dailyReport.setBillingOvertimeUnitPrice(
                nvl(rate.getOvertimeUnitPrice())
        );

        dailyReport.setBillingNightUnitPrice(
                nvl(rate.getNightUnitPrice())
        );

        dailyReport.setBillingHolidayUnitPrice(
                nvl(rate.getHolidayUnitPrice())
        );

        dailyReport.setBillingCommuteUnitPrice(
                resolveDistanceBillingUnitPrice(dailyReport)
        );
    }

    public void clearBillingRate(
            DailyReport dailyReport
    ) {
        dailyReport.setBillingRateId(null);
        dailyReport.setBillingUnit(null);

        dailyReport.setBillingBaseUnitPrice(
                BigDecimal.ZERO
        );

        dailyReport.setBillingOvertimeUnitPrice(
                BigDecimal.ZERO
        );

        dailyReport.setBillingNightUnitPrice(
                BigDecimal.ZERO
        );

        dailyReport.setBillingHolidayUnitPrice(
                BigDecimal.ZERO
        );

        dailyReport.setBillingCommuteUnitPrice(
                BigDecimal.ZERO
        );
    }

    private String normalizeSiteRoleCode(
            String siteRoleCode
    ) {
        if (siteRoleCode == null
                || siteRoleCode.isBlank()) {
            return DEFAULT_SITE_ROLE_CODE;
        }

        return siteRoleCode.trim();
    }

    public String defaultSiteRoleCode() {
        return DEFAULT_SITE_ROLE_CODE;
    }

    public String defaultSiteRoleName() {
        return DEFAULT_SITE_ROLE_NAME;
    }

    private BigDecimal resolveDistanceBillingUnitPrice(DailyReport dailyReport) {
        VehicleArrangementType arrangementType = dailyReport.getVehicleArrangementType();
        if (arrangementType == null
                || arrangementType == VehicleArrangementType.NONE
                || arrangementType == VehicleArrangementType.PASSENGER
                || dailyReport.getCustomerId() == null) {
            return BigDecimal.ZERO;
        }
        return customerRepository.findByIdAndDeletedAtIsNull(dailyReport.getCustomerId())
                .map(customer -> nvl(customer.getDistanceBillingUnitPrice()))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal nvl(
            BigDecimal value
    ) {
        return value != null
                ? value
                : BigDecimal.ZERO;
    }
}
