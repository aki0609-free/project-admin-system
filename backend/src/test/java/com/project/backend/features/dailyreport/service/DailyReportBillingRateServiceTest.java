package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.entity.CustomerSiteBillingRate;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.service.CustomerSiteBillingRateQueryService;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.enums.VehicleArrangementType;

class DailyReportBillingRateServiceTest {

    private final CustomerSiteBillingRateQueryService rateQuery =
            mock(CustomerSiteBillingRateQueryService.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final DailyReportBillingRateService service =
            new DailyReportBillingRateService(rateQuery, customerRepository);

    @Test
    void companyArrangementUsesCustomerDistanceRate() {
        DailyReport report = report(VehicleArrangementType.COMPANY);
        Customer customer = new Customer();
        customer.setId(10L);
        customer.setDistanceBillingUnitPrice(new BigDecimal("30.00"));
        when(rateQuery.findApplicableRate(20L, "WORKER", "GENERAL", report.getWorkDate()))
                .thenReturn(rate());
        when(customerRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(customer));

        service.applyBillingRate(report);

        assertThat(report.getBillingCommuteUnitPrice())
                .isEqualByComparingTo("30.00");
    }

    @Test
    void employeeArrangementAlsoUsesCustomerDistanceRate() {
        DailyReport report = report(VehicleArrangementType.EMPLOYEE);
        Customer customer = new Customer();
        customer.setId(10L);
        customer.setDistanceBillingUnitPrice(new BigDecimal("30.00"));
        when(rateQuery.findApplicableRate(20L, "WORKER", "GENERAL", report.getWorkDate()))
                .thenReturn(rate());
        when(customerRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(customer));

        service.applyBillingRate(report);

        assertThat(report.getBillingCommuteUnitPrice())
                .isEqualByComparingTo("30.00");
    }

    @Test
    void noVehicleArrangementDoesNotCreateCustomerDistanceCharge() {
        DailyReport report = report(VehicleArrangementType.NONE);
        when(rateQuery.findApplicableRate(20L, "WORKER", "GENERAL", report.getWorkDate()))
                .thenReturn(rate());

        service.applyBillingRate(report);

        assertThat(report.getBillingCommuteUnitPrice()).isZero();
    }

    @Test
    void passengerDoesNotCreateDuplicateCustomerDistanceCharge() {
        DailyReport report = report(VehicleArrangementType.PASSENGER);
        when(rateQuery.findApplicableRate(20L, "WORKER", "GENERAL", report.getWorkDate()))
                .thenReturn(rate());

        service.applyBillingRate(report);

        assertThat(report.getBillingCommuteUnitPrice()).isZero();
    }

    private DailyReport report(VehicleArrangementType arrangementType) {
        DailyReport report = new DailyReport();
        report.setCustomerId(10L);
        report.setCustomerSiteId(20L);
        report.setJobCode("WORKER");
        report.setSiteRoleCode("GENERAL");
        report.setWorkDate(LocalDate.of(2026, 9, 14));
        report.setVehicleArrangementType(arrangementType);
        return report;
    }

    private CustomerSiteBillingRate rate() {
        CustomerSiteBillingRate rate = new CustomerSiteBillingRate();
        rate.setId(30L);
        rate.setJobCode("WORKER");
        rate.setJobName("作業員");
        rate.setSiteRoleCode("GENERAL");
        rate.setSiteRoleName("一般");
        rate.setBaseUnitPrice(BigDecimal.TEN);
        rate.setOvertimeUnitPrice(BigDecimal.ZERO);
        rate.setNightUnitPrice(BigDecimal.ZERO);
        rate.setHolidayUnitPrice(BigDecimal.ZERO);
        rate.setCommuteUnitPrice(new BigDecimal("999"));
        return rate;
    }
}
