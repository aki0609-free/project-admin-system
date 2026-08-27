package com.project.backend.features.operation.monthly.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.enums.CustomerInvoiceType;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.service.resolver.InvoiceReportCodeResolver;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;
import com.project.backend.features.operation.monthly.service.executor.MonthlyClosingJobExecutor;
import com.project.backend.features.operation.book.service.SpreadsheetLedgerGenerationService;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;

class MonthlyClosingJobServiceTest {

    private OperationReportPreviewRepository previewRepository;
    private MonthlyClosingOutputDefinitionRepository outputDefinitionRepository;
    private CustomerRepository customerRepository;
    private MonthlyInvoiceTargetCustomerQueryService
            invoiceTargetCustomerQueryService;
    private InvoiceReportCodeResolver reportCodeResolver;
    private MonthlyClosingJobExecutor executor;
    private SpreadsheetLedgerGenerationService ledgerGenerationService;
    private MonthlyClosingCustomerTransactionService transactionService;
    private MonthlyClosingJobService service;

    @BeforeEach
    void setUp() {
        previewRepository = mock(OperationReportPreviewRepository.class);
        outputDefinitionRepository = mock(
                MonthlyClosingOutputDefinitionRepository.class
        );
        customerRepository = mock(CustomerRepository.class);
        invoiceTargetCustomerQueryService = mock(
                MonthlyInvoiceTargetCustomerQueryService.class
        );
        reportCodeResolver = mock(InvoiceReportCodeResolver.class);
        executor = mock(MonthlyClosingJobExecutor.class);
        ledgerGenerationService = mock(
                SpreadsheetLedgerGenerationService.class
        );
        transactionService = mock(
                MonthlyClosingCustomerTransactionService.class
        );
        service = new MonthlyClosingJobService(
                previewRepository,
                outputDefinitionRepository,
                executor,
                ledgerGenerationService
        );
        when(outputDefinitionRepository
                .findByOutputTypeAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc(
                        MonthlyClosingOutputType.REPORT
                )).thenReturn(List.of());
        when(outputDefinitionRepository
                .findByOutputTypeAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc(
                        MonthlyClosingOutputType.LEDGER
                )).thenReturn(List.of());
    }

    @Test
    void executeClosing_shouldLeaveInvoiceToCustomerBillingClosing() {
        OperationReportPreview paySlip = preview(
                "MONTHLY_PAY_SLIP",
                "PRINT_MONTHLY_PAY_SLIP"
        );
        OperationReportPreview invoice = preview(
                "MONTHLY_INVOICE",
                "PRINT_MONTHLY_INVOICE"
        );
        Customer first = customer(1L, CustomerInvoiceType.PATTERN_1);
        Customer second = customer(2L, CustomerInvoiceType.PATTERN_2);
        MonthlyClosingPeriod period = period();

        when(previewRepository
                .findByOperationTypeAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        OperationType.MONTHLY
                )).thenReturn(List.of(paySlip, invoice));
        when(invoiceTargetCustomerQueryService.findTargetCustomerIds(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).thenReturn(List.of(1L, 2L));
        when(customerRepository
                .findByIdInAndDeletedAtIsNullOrderByIdAsc(
                        List.of(1L, 2L)
                ))
                .thenReturn(List.of(first, second));
        when(reportCodeResolver.resolve(CustomerInvoiceType.PATTERN_1))
                .thenReturn("MONTHLY_INVOICE_PATTERN_1");
        when(reportCodeResolver.resolve(CustomerInvoiceType.PATTERN_2))
                .thenReturn("MONTHLY_INVOICE_PATTERN_2");

        service.executeClosing(10L, period, 3);

        InOrder order = inOrder(executor);
        order.verify(executor).execute(10L, paySlip, period, 3);
        verify(executor, never()).execute(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.eq(invoice),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(transactionService, never()).synchronize("2026-07", 3);
    }

    @Test
    void executeClosing_shouldRejectEmptyReportConfiguration() {
        when(previewRepository
                .findByOperationTypeAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        OperationType.MONTHLY
                )).thenReturn(List.of());

        assertThatThrownBy(() -> service.executeClosing(
                10L,
                period(),
                1
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("月次締め帳票");
        verify(executor, never()).execute(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void executeClosing_shouldSkipInvoiceAndTransactionWhenNoBillingTarget() {
        OperationReportPreview invoice = preview(
                "MONTHLY_INVOICE",
                "PRINT_MONTHLY_INVOICE"
        );
        when(previewRepository
                .findByOperationTypeAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        OperationType.MONTHLY
                )).thenReturn(List.of(invoice));
        when(invoiceTargetCustomerQueryService.findTargetCustomerIds(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).thenReturn(List.of());

        service.executeClosing(10L, period(), 1);

        verify(transactionService, never()).synchronize(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void executeClosing_shouldUseConfiguredClosingReportsInConfiguredOrder() {
        OperationReportPreview invoice = preview(
                "MONTHLY_INVOICE",
                "PRINT_MONTHLY_INVOICE"
        );
        invoice.setActiveFlag(true);
        OperationReportPreview paySlip = preview(
                "MONTHLY_PAY_SLIP",
                "PRINT_MONTHLY_PAY_SLIP"
        );
        paySlip.setActiveFlag(true);

        MonthlyClosingOutputDefinition first = definition(
                "MONTHLY_INVOICE",
                true
        );
        MonthlyClosingOutputDefinition second = definition(
                "MONTHLY_PAY_SLIP",
                true
        );
        when(outputDefinitionRepository
                .findByOutputTypeAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc(
                        MonthlyClosingOutputType.REPORT
                )).thenReturn(List.of(first, second));
        when(previewRepository
                .findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                        OperationType.MONTHLY,
                        "MONTHLY_INVOICE"
                )).thenReturn(java.util.Optional.of(invoice));
        when(previewRepository
                .findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                        OperationType.MONTHLY,
                        "MONTHLY_PAY_SLIP"
                )).thenReturn(java.util.Optional.of(paySlip));
        when(invoiceTargetCustomerQueryService.findTargetCustomerIds(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31)
        )).thenReturn(List.of());

        service.executeClosing(10L, period(), 1);

        verify(executor).execute(10L, paySlip, period(), 1);
    }

    @Test
    void executeClosing_shouldGenerateConfiguredLedgers() {
        OperationReportPreview paySlip = preview(
                "MONTHLY_PAY_SLIP",
                "PRINT_MONTHLY_PAY_SLIP"
        );
        when(previewRepository
                .findByOperationTypeAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                        OperationType.MONTHLY
                )).thenReturn(List.of(paySlip));

        MonthlyClosingOutputDefinition monthlyLabor =
                ledgerDefinition("MONTHLY_LABOR", true);
        MonthlyClosingOutputDefinition disabled =
                ledgerDefinition("DISABLED_LEDGER", false);
        when(outputDefinitionRepository
                .findByOutputTypeAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc(
                        MonthlyClosingOutputType.LEDGER
                )).thenReturn(List.of(monthlyLabor, disabled));

        service.executeClosing(10L, period(), 3);

        verify(ledgerGenerationService).generateForClosing(
                "MONTHLY_LABOR",
                "2026-07",
                3
        );
        verify(ledgerGenerationService, never()).generateForClosing(
                "DISABLED_LEDGER",
                "2026-07",
                3
        );
    }

    private OperationReportPreview preview(
            String reportCode,
            String jobCode
    ) {
        OperationReportPreview preview = new OperationReportPreview();
        preview.setReportCode(reportCode);
        preview.setJobCode(jobCode);
        return preview;
    }

    private MonthlyClosingOutputDefinition definition(
            String reportCode,
            boolean active
    ) {
        MonthlyClosingOutputDefinition definition =
                new MonthlyClosingOutputDefinition();
        definition.setOutputType(MonthlyClosingOutputType.REPORT);
        definition.setOutputCode(reportCode);
        definition.setActiveFlag(active);
        return definition;
    }

    private MonthlyClosingOutputDefinition ledgerDefinition(
            String bookCode,
            boolean active
    ) {
        MonthlyClosingOutputDefinition definition =
                new MonthlyClosingOutputDefinition();
        definition.setOutputType(MonthlyClosingOutputType.LEDGER);
        definition.setOutputCode(bookCode);
        definition.setActiveFlag(active);
        return definition;
    }

    private Customer customer(
            Long id,
            CustomerInvoiceType invoiceType
    ) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("顧客" + id);
        customer.setInvoiceType(invoiceType);
        return customer;
    }

    private MonthlyClosingPeriod period() {
        return new MonthlyClosingPeriod(
                "2026-07",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                null
        );
    }
}
