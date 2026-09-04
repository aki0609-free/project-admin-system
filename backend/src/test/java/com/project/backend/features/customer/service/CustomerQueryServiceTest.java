package com.project.backend.features.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.entity.CustomerSite;
import com.project.backend.features.customer.enums.CustomerContractStatus;
import com.project.backend.features.customer.mapper.CustomerMapper;
import com.project.backend.features.customer.repository.CustomerEmployeeRepository;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerSiteRepository;
import com.project.backend.features.customer.repository.CustomerTransactionRepository;

class CustomerQueryServiceTest {

    @Test
    void findOptions_shouldReturnOnlyActiveCustomersAndTheirSites() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        CustomerSiteRepository siteRepository = mock(CustomerSiteRepository.class);
        Customer active = customer(1L, "契約中", CustomerContractStatus.ACTIVE);
        Customer ended = customer(2L, "契約終了", CustomerContractStatus.ENDED);
        when(customerRepository.findByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(List.of(active, ended));
        when(siteRepository.findByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(List.of(site(10L, 1L, "現場A"), site(20L, 2L, "現場B")));

        CustomerQueryService service = new CustomerQueryService(
                customerRepository,
                siteRepository,
                mock(CustomerEmployeeRepository.class),
                mock(CustomerTransactionRepository.class),
                mock(CustomerMapper.class)
        );

        var result = service.findOptions();

        assertThat(result.customers()).extracting("id").containsExactly(1L);
        assertThat(result.sites()).extracting("id").containsExactly(10L);
    }

    private Customer customer(Long id, String name, CustomerContractStatus status) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName(name);
        customer.setContractFlag(status);
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
