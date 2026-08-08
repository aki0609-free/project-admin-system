package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReport;

class DailyReportCustomerSiteResolverTest {

    private final CustomerRepository customerRepository =
            mock(CustomerRepository.class);
    private final CustomerSiteRepository siteRepository =
            mock(CustomerSiteRepository.class);
    private final DailyReportCustomerSiteResolver resolver =
            new DailyReportCustomerSiteResolver(customerRepository, siteRepository);

    @Test
    void applySnapshot_shouldUseCanonicalCustomerAndSiteNames() {
        Customer customer = customer(10L, "正式顧客名");
        CustomerSite site = site(20L, 10L, "正式現場名");
        when(siteRepository.findByIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(site));
        when(customerRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(customer));

        DailyReportSaveRequest request = mock(DailyReportSaveRequest.class);
        when(request.customerId()).thenReturn(10L);
        when(request.customerSiteId()).thenReturn(20L);
        DailyReport report = new DailyReport();

        resolver.applySnapshot(report, request);

        assertThat(report.getCustomerId()).isEqualTo(10L);
        assertThat(report.getCustomerName()).isEqualTo("正式顧客名");
        assertThat(report.getCustomerSiteId()).isEqualTo(20L);
        assertThat(report.getSiteName()).isEqualTo("正式現場名");
    }

    @Test
    void applySnapshot_shouldRejectSiteOwnedByDifferentCustomer() {
        CustomerSite site = site(20L, 99L, "別会社現場");
        when(siteRepository.findByIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(site));

        DailyReportSaveRequest request = mock(DailyReportSaveRequest.class);
        when(request.customerId()).thenReturn(10L);
        when(request.customerSiteId()).thenReturn(20L);

        assertThatThrownBy(() -> resolver.applySnapshot(new DailyReport(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("組み合わせが一致しません");
    }

    private Customer customer(Long id, String name) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName(name);
        return customer;
    }

    private CustomerSite site(Long id, Long customerId, String name) {
        CustomerSite site = new CustomerSite();
        site.setId(id);
        site.setCustomerId(customerId);
        site.setName(name);
        return site;
    }
}
