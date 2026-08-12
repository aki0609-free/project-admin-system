package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.operation.monthly.dto.CustomerBillingPeriod;
import com.project.backend.features.operation.monthly.repository.CustomerBillingClosingRepository;
import com.project.backend.features.operation.monthly.service.CustomerBillingTargetService.Target;

class CustomerBillingClosingCommandServiceTest {

    @Test
    void closeAllEligible_shouldExcludeCustomersBeforeTheirClosingDate() {
        CustomerBillingClosingRepository repository =
                mock(CustomerBillingClosingRepository.class);
        CustomerBillingTargetService targetService =
                mock(CustomerBillingTargetService.class);
        CustomerBillingClosingExecutionService executionService =
                mock(CustomerBillingClosingExecutionService.class);
        CustomerBillingClosingCommandService service =
                new CustomerBillingClosingCommandService(
                        repository,
                        targetService,
                        executionService,
                        Clock.fixed(
                                Instant.parse("2026-07-20T03:00:00Z"),
                                ZoneId.of("Asia/Tokyo")
                        )
                );
        Target twentiethClosing = target(1L, "20日締め", LocalDate.of(2026, 7, 20));
        Target monthEndClosing = target(2L, "月末締め", LocalDate.of(2026, 7, 31));

        when(targetService.findTargets("2026-07"))
                .thenReturn(List.of(twentiethClosing, monthEndClosing));
        when(repository.findAllByTargetMonthAndCustomerIdInAndDeletedAtIsNull(
                LocalDate.of(2026, 7, 1),
                List.of(1L, 2L)
        )).thenReturn(List.of());

        var result = service.closeAllEligible("2026-07");

        verify(executionService).execute("2026-07", 1L, false);
        verify(executionService, never()).execute("2026-07", 2L, false);
        assertThat(result.completedCount()).isEqualTo(1);
        assertThat(result.skippedBeforeClosingDateCount()).isEqualTo(1);
        assertThat(result.failedCount()).isZero();
    }

    private Target target(Long customerId, String name, LocalDate endDate) {
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(customerId);
        when(customer.getName()).thenReturn(name);
        return new Target(
                customer,
                new CustomerBillingPeriod(
                        "2026-07",
                        endDate.minusMonths(1).plusDays(1),
                        endDate,
                        null
                )
        );
    }
}
