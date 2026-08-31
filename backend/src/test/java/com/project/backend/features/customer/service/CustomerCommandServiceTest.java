package com.project.backend.features.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.customer.dto.CustomerEmployeeRequest;
import com.project.backend.features.customer.dto.CustomerSaveRequest;
import com.project.backend.features.customer.dto.CustomerSiteRequest;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.enums.CustomerInvoiceType;
import com.project.backend.features.customer.mapper.CustomerMapper;
import com.project.backend.features.customer.repository.CustomerEmployeeRepository;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerSiteBillingRateRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;
import com.project.backend.features.customer.service.integration.CustomerInvoiceMailGroupSynchronizer;

class CustomerCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-09T00:00:00Z");

    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final CustomerSiteRepository siteRepository = mock(CustomerSiteRepository.class);
    private final CustomerEmployeeRepository employeeRepository = mock(CustomerEmployeeRepository.class);
    private final CustomerSiteBillingRateRepository billingRateRepository =
            mock(CustomerSiteBillingRateRepository.class);
    private final CustomerReferenceGuard referenceGuard = mock(CustomerReferenceGuard.class);
    private final CustomerInvoiceMailGroupSynchronizer invoiceMailGroupSynchronizer =
            mock(CustomerInvoiceMailGroupSynchronizer.class);
    private final CustomerCommandService service = new CustomerCommandService(
            customerRepository,
            siteRepository,
            employeeRepository,
            billingRateRepository,
            new CustomerMapper(),
            referenceGuard,
            invoiceMailGroupSynchronizer,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void update_shouldRejectSiteOwnedByAnotherCustomer() {
        Customer customer = customer(10L);
        CustomerSite foreignSite = new CustomerSite();
        foreignSite.setId(20L);
        foreignSite.setCustomerId(99L);

        when(customerRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(customer));
        when(siteRepository.findByIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(foreignSite));

        assertThatThrownBy(() -> service.update(
                10L,
                request(List.of(new CustomerSiteRequest(
                        20L, "他社現場", null, null, null, null,
                        false, false, true
                )), List.of())
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("顧客IDが一致しません");

        verify(siteRepository, never()).deleteById(20L);
    }

    @Test
    void create_shouldRequireEmailForInvoiceRecipient() {
        CustomerEmployeeRequest recipient = new CustomerEmployeeRequest(
                null, "担当者", null, null, null, null,
                true, false, true, false, false
        );

        assertThatThrownBy(() -> service.create(
                request(List.of(), List.of(recipient))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("メールアドレス");

        verify(customerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void create_shouldRejectSameEmployeeAsToAndCc() {
        CustomerEmployeeRequest recipient = new CustomerEmployeeRequest(
                null, "担当者", null, null, null, "recipient@example.com",
                true, true, true, false, false
        );

        assertThatThrownBy(() -> service.create(
                request(List.of(), List.of(recipient))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("同時");
    }

    @Test
    void create_shouldRejectInvalidSiteEmailAndDistance() {
        CustomerSiteRequest invalidEmail = new CustomerSiteRequest(
                null, "本社現場", null, null, "invalid-email", null,
                true, false, false
        );
        assertThatThrownBy(() -> service.create(
                request(List.of(invalidEmail), List.of())
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("メールアドレス");

        CustomerSiteRequest invalidDistance = new CustomerSiteRequest(
                null, "本社現場", null, null, null, -1,
                true, false, false
        );
        assertThatThrownBy(() -> service.create(
                request(List.of(invalidDistance), List.of())
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("距離");

        verify(customerRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void create_shouldRejectOutOfRangeClosingDay() {
        CustomerSaveRequest request = new CustomerSaveRequest(
                "テスト顧客", null, null, null, null, null, null,
                null, null, CustomerInvoiceType.PATTERN_1,
                new DayRule(DayRuleType.DAY_OF_MONTH, 32, 0),
                null, List.of(), List.of()
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1日から31日");
    }

    @Test
    void delete_shouldSoftDeleteUnreferencedCustomer() {
        Customer customer = customer(10L);
        when(customerRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(customer));
        when(siteRepository.findByCustomerIdAndDeletedAtIsNullOrderByIdAsc(10L))
                .thenReturn(List.of());
        when(employeeRepository.findByCustomerIdAndDeletedAtIsNullOrderByIdAsc(10L))
                .thenReturn(List.of());

        service.delete(10L);

        verify(referenceGuard).assertCustomerDeletable(10L);
        assertThat(customer.getDeletedAt()).isEqualTo(NOW);
        verify(invoiceMailGroupSynchronizer).delete(10L);
        verify(customerRepository, never()).delete(customer);
    }

    private Customer customer(Long id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("テスト顧客");
        return customer;
    }

    private CustomerSaveRequest request(
            List<CustomerSiteRequest> sites,
            List<CustomerEmployeeRequest> employees
    ) {
        return new CustomerSaveRequest(
                "テスト顧客", null, null, null, null, null, null,
                null, null, CustomerInvoiceType.PATTERN_1,
                null, null, sites, employees
        );
    }
}
