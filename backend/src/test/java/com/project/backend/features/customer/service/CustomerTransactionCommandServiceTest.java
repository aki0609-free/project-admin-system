package com.project.backend.features.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.customer.dto.CustomerPaymentConfirmRequest;
import com.project.backend.features.customer.entity.CustomerTransaction;
import com.project.backend.features.customer.enums.CustomerPaymentStatus;
import com.project.backend.features.customer.mapper.CustomerTransactionMapper;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.repository.CustomerTransactionRepository;

class CustomerTransactionCommandServiceTest {

    private final CustomerRepository customerRepository =
            mock(CustomerRepository.class);
    private final CustomerTransactionRepository repository =
            mock(CustomerTransactionRepository.class);
    private final CustomerTransactionCommandService service =
            new CustomerTransactionCommandService(
                    customerRepository,
                    repository,
                    new CustomerTransactionMapper()
            );

    @Test
    void confirmPayment_shouldCountFeeAndOffsetAsSettledAmount() {
        CustomerTransaction entity = transaction(1_134_014);
        when(customerRepository.existsById(10L)).thenReturn(true);
        when(repository.findById(20L)).thenReturn(Optional.of(entity));

        service.confirmPayment(
                10L,
                20L,
                new CustomerPaymentConfirmRequest(
                        LocalDate.of(2026, 3, 31),
                        1_133_464,
                        550,
                        0,
                        null
                )
        );

        assertThat(entity.getTotalAmount()).isEqualTo(1_134_014);
        assertThat(entity.getPaymentStatus())
                .isEqualTo(CustomerPaymentStatus.PAID);
    }

    @Test
    void confirmPayment_shouldKeepPartialStatusWhenSettlementIsShort() {
        CustomerTransaction entity = transaction(100_000);
        when(customerRepository.existsById(10L)).thenReturn(true);
        when(repository.findById(20L)).thenReturn(Optional.of(entity));

        service.confirmPayment(
                10L,
                20L,
                new CustomerPaymentConfirmRequest(
                        LocalDate.of(2026, 3, 31),
                        80_000,
                        500,
                        1_000,
                        null
                )
        );

        assertThat(entity.getTotalAmount()).isEqualTo(81_500);
        assertThat(entity.getPaymentStatus())
                .isEqualTo(CustomerPaymentStatus.PARTIAL);
    }

    private CustomerTransaction transaction(int billingAmount) {
        CustomerTransaction entity = new CustomerTransaction();
        entity.setId(20L);
        entity.setCustomerId(10L);
        entity.setTargetMonth("2026-02");
        entity.setBillingAmount(billingAmount);
        return entity;
    }
}
