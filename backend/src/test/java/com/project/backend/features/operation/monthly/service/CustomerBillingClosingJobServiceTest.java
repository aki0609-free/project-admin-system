package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.exception.CustomerTransactionAlreadySettledException;
import com.project.backend.features.operation.monthly.dto.CustomerBillingPeriod;
import com.project.backend.features.operation.monthly.service.CustomerBillingTargetService.Target;
import com.project.backend.features.operation.monthly.service.executor.MonthlyClosingJobExecutor;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;
import com.project.backend.features.customer.service.resolver.InvoiceReportCodeResolver;

class CustomerBillingClosingJobServiceTest {

    @Test
    void execute_shouldRejectSettledTransactionBeforeGeneratingDocuments() {
        OperationReportPreviewRepository previewRepository =
                mock(OperationReportPreviewRepository.class);
        InvoiceReportCodeResolver invoiceReportCodeResolver =
                mock(InvoiceReportCodeResolver.class);
        MonthlyClosingJobExecutor executor =
                mock(MonthlyClosingJobExecutor.class);
        MonthlyClosingCustomerTransactionService transactionService =
                mock(MonthlyClosingCustomerTransactionService.class);
        CustomerBillingClosingJobService service =
                new CustomerBillingClosingJobService(
                        previewRepository,
                        invoiceReportCodeResolver,
                        executor,
                        transactionService
                );
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(7L);
        Target target = new Target(
                customer,
                new CustomerBillingPeriod(
                        "2026-08",
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31),
                        null
                )
        );
        doThrow(new CustomerTransactionAlreadySettledException(7L, "2026-08"))
                .when(transactionService)
                .validateSynchronizable("2026-08", 7L);

        assertThatThrownBy(() -> service.execute(
                1L,
                "2026-08",
                1,
                target
        )).isInstanceOf(CustomerTransactionAlreadySettledException.class);

        verifyNoInteractions(executor);
        verifyNoInteractions(previewRepository);
    }
}
