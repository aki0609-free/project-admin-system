package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.operation.monthly.dto.CustomerBillingPeriod;
import com.project.backend.features.operation.monthly.entity.CustomerBillingClosing;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.CustomerBillingClosingRepository;
import com.project.backend.features.operation.monthly.service.CustomerBillingTargetService.Target;

class CustomerBillingClosingExecutionServiceTest {

    private CustomerBillingClosingRepository repository;
    private CustomerBillingTargetService targetService;
    private CustomerBillingClosingJobService jobService;
    private CustomerBillingClosingExecutionService service;
    private CustomerBillingClosing entity;
    private Target target;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerBillingClosingRepository.class);
        targetService = mock(CustomerBillingTargetService.class);
        jobService = mock(CustomerBillingClosingJobService.class);
        entity = new CustomerBillingClosing();
        entity.setId(20L);
        entity.setTargetMonth(LocalDate.of(2026, 7, 1));
        entity.setCustomerId(30L);
        entity.setStatus(MonthlyClosingStatus.OPEN);
        entity.setClosingVersion(0);

        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(30L);
        target = new Target(
                customer,
                new CustomerBillingPeriod(
                        "2026-07",
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31),
                        null
                )
        );

        when(repository.findByTargetMonthAndCustomerIdAndDeletedAtIsNull(
                LocalDate.of(2026, 7, 1),
                30L
        )).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(targetService.findTarget("2026-07", 30L)).thenReturn(target);

        service = new CustomerBillingClosingExecutionService(
                repository,
                targetService,
                jobService,
                Clock.fixed(
                        Instant.parse("2026-08-01T00:00:00Z"),
                        ZoneId.of("Asia/Tokyo")
                )
        );
    }

    @Test
    void close_shouldMarkOnlyTheCustomerClosedAfterDocumentsComplete() {
        service.execute("2026-07", 30L, false);

        verify(jobService).execute(20L, "2026-07", 1, target);
        assertThat(entity.getStatus()).isEqualTo(MonthlyClosingStatus.CLOSED);
        assertThat(entity.getClosingVersion()).isEqualTo(1);
        assertThat(entity.getClosedAt())
                .isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
    }

    @Test
    void close_shouldNotMarkCustomerClosedWhenDocumentGenerationFails() {
        doThrow(new IllegalStateException("注文書生成失敗"))
                .when(jobService)
                .execute(20L, "2026-07", 1, target);

        assertThatThrownBy(() -> service.execute("2026-07", 30L, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("注文書生成失敗");
        assertThat(entity.getStatus()).isEqualTo(MonthlyClosingStatus.OPEN);
        assertThat(entity.getClosingVersion()).isZero();
    }
}
