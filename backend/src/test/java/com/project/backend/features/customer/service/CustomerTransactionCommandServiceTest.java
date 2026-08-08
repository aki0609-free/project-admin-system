package com.project.backend.features.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.customer.dto.CustomerPaymentConfirmRequest;
import com.project.backend.features.customer.dto.CustomerTransactionClosingRequest;
import com.project.backend.features.customer.dto.CustomerTransactionRequest;
import com.project.backend.features.customer.entity.Customer;
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
        customerExists();
        when(repository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(entity));

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
        customerExists();
        when(repository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(entity));

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

    @Test
    void upsertFromMonthlyClosing_shouldRefreshPartialPaymentStatus() {
        CustomerTransaction entity = transaction(100_000);
        entity.setPaidAmount(80_000);
        entity.setFee(500);
        entity.setOffsetAmount(1_000);
        entity.setPaymentStatus(CustomerPaymentStatus.PARTIAL);
        customerExists();
        when(repository.findByCustomerIdAndTargetMonthAndDeletedAtIsNull(
                10L,
                "2026-02"
        )).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        service.upsertFromMonthlyClosing(
                closingRequest(80_000, 90L, 2)
        );

        assertThat(entity.getTotalAmount()).isEqualTo(81_500);
        assertThat(entity.getPaymentStatus())
                .isEqualTo(CustomerPaymentStatus.OVERPAID);
        assertThat(entity.getSourceInvoiceHistoryId()).isEqualTo(90L);
        assertThat(entity.getSourceClosingVersion()).isEqualTo(2);
    }

    @Test
    void upsertFromMonthlyClosing_shouldRejectPaidTransaction() {
        CustomerTransaction entity = transaction(100_000);
        entity.setPaymentStatus(CustomerPaymentStatus.PAID);
        customerExists();
        when(repository.findByCustomerIdAndTargetMonthAndDeletedAtIsNull(
                10L,
                "2026-02"
        )).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.upsertFromMonthlyClosing(
                closingRequest(110_000, 91L, 2)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("入金済み");
        verify(repository, never()).save(entity);
    }

    @Test
    void create_shouldRejectDuplicateCustomerAndTargetMonth() {
        customerExists();
        when(repository.existsByCustomerIdAndTargetMonthAndDeletedAtIsNull(
                10L,
                "2026-02"
        )).thenReturn(true);

        assertThatThrownBy(() -> service.create(10L, transactionRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("既に登録");

        verify(repository, never()).save(
                org.mockito.ArgumentMatchers.any(CustomerTransaction.class)
        );
    }

    @Test
    void delete_shouldRejectTransactionCreatedByMonthlyClosing() {
        CustomerTransaction entity = transaction(100_000);
        entity.setSourceType("MONTHLY_CLOSING");
        customerExists();
        when(repository.findByIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.delete(10L, 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("月次締め");

        verify(repository, never()).delete(entity);
    }

    private CustomerTransactionClosingRequest closingRequest(
            int billingAmount,
            long historyId,
            int closingVersion
    ) {
        return new CustomerTransactionClosingRequest(
                10L,
                "2026-02",
                null,
                null,
                billingAmount,
                null,
                null,
                historyId,
                closingVersion
        );
    }

    private CustomerTransaction transaction(int billingAmount) {
        CustomerTransaction entity = new CustomerTransaction();
        entity.setId(20L);
        entity.setCustomerId(10L);
        entity.setTargetMonth("2026-02");
        entity.setBillingAmount(billingAmount);
        return entity;
    }

    private void customerExists() {
        Customer customer = new Customer();
        customer.setId(10L);
        when(customerRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(customer));
    }

    private CustomerTransactionRequest transactionRequest() {
        return new CustomerTransactionRequest(
                null,
                10L,
                "2026-02",
                null,
                null,
                100_000,
                null,
                null,
                0,
                0,
                0,
                0,
                CustomerPaymentStatus.UNPAID,
                null
        );
    }
}
