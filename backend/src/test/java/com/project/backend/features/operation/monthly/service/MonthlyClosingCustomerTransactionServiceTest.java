package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.customer.dto.CustomerTransactionClosingRequest;
import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.service.CustomerTransactionCommandService;
import com.project.backend.features.system.report.invoice.entity.MonthlyInvoiceHistory;
import com.project.backend.features.system.report.invoice.repository.MonthlyInvoiceHistoryRepository;

class MonthlyClosingCustomerTransactionServiceTest {

    private MonthlyInvoiceHistoryRepository historyRepository;
    private CustomerRepository customerRepository;
    private CustomerTransactionCommandService commandService;
    private MonthlyClosingCustomerTransactionService service;

    @BeforeEach
    void setUp() {
        historyRepository = mock(MonthlyInvoiceHistoryRepository.class);
        customerRepository = mock(CustomerRepository.class);
        commandService = mock(CustomerTransactionCommandService.class);
        service = new MonthlyClosingCustomerTransactionService(
                historyRepository,
                customerRepository,
                commandService
        );
    }

    @Test
    void synchronize_shouldUseFixedInvoiceAmountAndCustomerRules() {
        MonthlyInvoiceHistory history = new MonthlyInvoiceHistory(
                91L,
                LocalDate.of(2026, 7, 1),
                2,
                7L,
                new BigDecimal("123400.00")
        );
        Customer customer = new Customer();
        customer.setId(7L);
        customer.setClosingDayType(DayRuleType.END_OF_MONTH);
        customer.setClosingMonthOffset(0);
        customer.setPaymentDayType(DayRuleType.DAY_OF_MONTH);
        customer.setPaymentDayValue(20);
        customer.setPaymentMonthOffset(1);

        when(historyRepository
                .findByTargetMonthAndClosingVersionAndDeletedAtIsNullOrderByCustomerIdAsc(
                        LocalDate.of(2026, 7, 1),
                        2
                )).thenReturn(List.of(history));
        when(customerRepository.findById(7L))
                .thenReturn(Optional.of(customer));

        int count = service.synchronize("2026-07", 2);

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<CustomerTransactionClosingRequest> captor =
                ArgumentCaptor.forClass(
                        CustomerTransactionClosingRequest.class
                );
        verify(commandService).upsertFromMonthlyClosing(
                captor.capture()
        );
        CustomerTransactionClosingRequest request = captor.getValue();
        assertThat(request.billingAmount()).isEqualTo(123400);
        assertThat(request.expectedPaymentDate())
                .isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(request.sourceInvoiceHistoryId()).isEqualTo(91L);
        assertThat(request.sourceClosingVersion()).isEqualTo(2);
    }

    @Test
    void synchronize_shouldRejectMissingFixedInvoiceHistory() {
        when(historyRepository
                .findByTargetMonthAndClosingVersionAndDeletedAtIsNullOrderByCustomerIdAsc(
                        LocalDate.of(2026, 7, 1),
                        1
                )).thenReturn(List.of());

        assertThatThrownBy(() -> service.synchronize("2026-07", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("確定請求履歴");
    }

    @Test
    void synchronize_shouldRejectFractionalYen() {
        MonthlyInvoiceHistory history = new MonthlyInvoiceHistory(
                92L,
                LocalDate.of(2026, 7, 1),
                1,
                8L,
                new BigDecimal("100.50")
        );
        Customer customer = new Customer();
        customer.setId(8L);
        when(historyRepository
                .findByTargetMonthAndClosingVersionAndDeletedAtIsNullOrderByCustomerIdAsc(
                        LocalDate.of(2026, 7, 1),
                        1
                )).thenReturn(List.of(history));
        when(customerRepository.findById(8L))
                .thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.synchronize("2026-07", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("円単位");
    }
}
