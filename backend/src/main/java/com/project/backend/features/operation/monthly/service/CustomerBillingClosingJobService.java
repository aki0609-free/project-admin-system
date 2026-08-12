package com.project.backend.features.operation.monthly.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.service.resolver.InvoiceReportCodeResolver;
import com.project.backend.features.operation.monthly.dto.CustomerBillingPeriod;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingReportTarget;
import com.project.backend.features.operation.monthly.service.CustomerBillingTargetService.Target;
import com.project.backend.features.operation.monthly.service.executor.MonthlyClosingJobExecutor;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerBillingClosingJobService {

    private static final String MONTHLY_INVOICE_CODE = "MONTHLY_INVOICE";
    private static final String MONTHLY_ORDER_FORM_CODE = "MONTHLY_ORDER_FORM";

    private final OperationReportPreviewRepository previewRepository;
    private final InvoiceReportCodeResolver invoiceReportCodeResolver;
    private final MonthlyClosingJobExecutor executor;
    private final MonthlyClosingCustomerTransactionService transactionService;

    public int execute(
            Long customerBillingClosingId,
            String targetMonth,
            Integer closingVersion,
            Target target
    ) {
        OperationReportPreview invoice = previewRepository
                .findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                        OperationType.MONTHLY,
                        MONTHLY_INVOICE_CODE
                )
                .filter(preview -> Boolean.TRUE.equals(preview.getActiveFlag()))
                .orElseThrow(() -> new IllegalStateException(
                        "有効な月次請求書が設定されていません。"
                ));
        OperationReportPreview orderForm = previewRepository
                .findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                        OperationType.MONTHLY,
                        MONTHLY_ORDER_FORM_CODE
                )
                .filter(preview -> Boolean.TRUE.equals(preview.getActiveFlag()))
                .orElseThrow(() -> new IllegalStateException(
                        "有効な月次注文書が設定されていません。"
                ));

        CustomerBillingPeriod billingPeriod = target.period();
        MonthlyClosingPeriod reportPeriod = new MonthlyClosingPeriod(
                billingPeriod.targetMonth(),
                billingPeriod.startDate(),
                billingPeriod.endDate(),
                billingPeriod.rule()
        );
        String reportCode = invoiceReportCodeResolver.resolve(
                target.customer().getInvoiceType()
        );
        MonthlyClosingReportTarget reportTarget = MonthlyClosingReportTarget.customer(
                target.customer().getId(),
                target.customer().getName()
        );
        executor.execute(
                customerBillingClosingId,
                invoice,
                reportPeriod,
                closingVersion,
                reportTarget,
                reportCode,
                "CUSTOMER_BILLING"
        );
        executor.execute(
                customerBillingClosingId,
                orderForm,
                reportPeriod,
                closingVersion,
                reportTarget,
                MONTHLY_ORDER_FORM_CODE,
                "CUSTOMER_BILLING"
        );
        transactionService.synchronize(
                targetMonth,
                closingVersion,
                target.customer().getId()
        );
        return 1;
    }
}
