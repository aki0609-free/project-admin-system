package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.enums.CustomerContractStatus;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.operation.monthly.dto.CustomerBillingPeriod;

class CustomerBillingTargetServiceTest {

    @Test
    void findTargets_shouldExcludeInactiveAndEndedCustomers() {
        CustomerRepository repository = mock(CustomerRepository.class);
        CustomerBillingPeriodService periodService = mock(CustomerBillingPeriodService.class);
        MonthlyInvoiceTargetCustomerQueryService targetQuery =
                mock(MonthlyInvoiceTargetCustomerQueryService.class);
        Customer active = customer(1L, CustomerContractStatus.ACTIVE);
        Customer inactive = customer(2L, CustomerContractStatus.INACTIVE);
        Customer ended = customer(3L, CustomerContractStatus.ENDED);
        when(repository.findByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(List.of(active, inactive, ended));
        when(targetQuery.findTargetCustomerIds(any(), any()))
                .thenReturn(List.of(1L, 2L, 3L));
        when(periodService.resolve("2026-08", active))
                .thenReturn(new CustomerBillingPeriod(
                        "2026-08",
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        null
                ));
        when(targetQuery.hasTargetData(anyLong(), any(), any())).thenReturn(true);

        CustomerBillingTargetService service = new CustomerBillingTargetService(
                repository, periodService, targetQuery
        );

        assertThat(service.findTargets("2026-08"))
                .extracting(target -> target.customer().getId())
                .containsExactly(1L);
    }

    private Customer customer(Long id, CustomerContractStatus status) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("顧客" + id);
        customer.setContractFlag(status);
        return customer;
    }
}
