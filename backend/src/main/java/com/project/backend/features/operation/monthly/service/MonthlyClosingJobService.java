package com.project.backend.features.operation.monthly.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.customer.service.resolver.InvoiceReportCodeResolver;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingReportTarget;
import com.project.backend.features.operation.monthly.entity.MonthlyClosingOutputDefinition;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingOutputDefinitionRepository;
import com.project.backend.features.operation.monthly.service.executor.MonthlyClosingJobExecutor;
import com.project.backend.features.operation.reportpreview.entity.OperationReportPreview;
import com.project.backend.features.operation.reportpreview.enums.OperationType;
import com.project.backend.features.operation.reportpreview.repository.OperationReportPreviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MonthlyClosingJobService {

    private static final String MONTHLY_INVOICE_JOB_CODE =
            "PRINT_MONTHLY_INVOICE";

    private final OperationReportPreviewRepository previewRepository;
    private final MonthlyClosingOutputDefinitionRepository outputDefinitionRepository;
    private final CustomerRepository customerRepository;
    private final MonthlyInvoiceTargetCustomerQueryService
            invoiceTargetCustomerQueryService;
    private final InvoiceReportCodeResolver invoiceReportCodeResolver;
    private final MonthlyClosingJobExecutor executor;
    private final MonthlyClosingCustomerTransactionService
            customerTransactionService;

    public void executeClosing(
            Long monthlyClosingId,
            MonthlyClosingPeriod period,
            Integer closingVersion
    ) {
        List<OperationReportPreview> previews = resolveClosingReports();
        if (previews.isEmpty()) {
            throw new IllegalStateException(
                    "有効な月次締め帳票が設定されていません。"
            );
        }

        boolean monthlyInvoiceExecuted = false;

        for (OperationReportPreview preview : previews) {
            String jobCode =
                    preview.getJobCode();

            if (jobCode == null || jobCode.isBlank()) {
                throw new IllegalStateException(
                        "月次締め帳票のjobCodeが未設定です。reportCode="
                                + preview.getReportCode()
                );
            }

            if (MONTHLY_INVOICE_JOB_CODE.equals(jobCode)) {
                int generatedInvoiceCount = executeMonthlyInvoice(
                        monthlyClosingId,
                        preview,
                        period,
                        closingVersion
                );
                monthlyInvoiceExecuted = generatedInvoiceCount > 0;

                continue;
            }

            executor.execute(
                    monthlyClosingId,
                    preview,
                    period,
                    closingVersion
            );
        }

        if (monthlyInvoiceExecuted) {
            customerTransactionService.synchronize(
                    period.targetMonth(),
                    closingVersion
            );
        }
    }

    private List<OperationReportPreview> resolveClosingReports() {
        List<MonthlyClosingOutputDefinition> definitions =
                outputDefinitionRepository
                        .findByOutputTypeAndDeletedAtIsNullOrderByExecutionOrderAscIdAsc(
                                MonthlyClosingOutputType.REPORT
                        );

        if (definitions.isEmpty()) {
            return previewRepository
                    .findByOperationTypeAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                            OperationType.MONTHLY
                    );
        }

        return definitions.stream()
                .filter(definition -> Boolean.TRUE.equals(definition.getActiveFlag()))
                .map(definition -> {
                    OperationReportPreview preview = previewRepository
                            .findByOperationTypeAndReportCodeAndDeletedAtIsNull(
                                    OperationType.MONTHLY,
                                    definition.getOutputCode()
                            )
                            .orElseThrow(() -> new IllegalStateException(
                                    "締め帳票に対応する月次帳票が見つかりません。reportCode="
                                            + definition.getOutputCode()
                            ));
                    if (!Boolean.TRUE.equals(preview.getActiveFlag())) {
                        throw new IllegalStateException(
                                "締め帳票が帳票管理側で無効です。reportCode="
                                        + definition.getOutputCode()
                        );
                    }
                    return preview;
                })
                .toList();
    }

    private int executeMonthlyInvoice(
            Long monthlyClosingId,
            OperationReportPreview preview,
            MonthlyClosingPeriod period,
            Integer closingVersion
    ) {
        List<Long> customerIds = invoiceTargetCustomerQueryService
                .findTargetCustomerIds(
                        period.startDate(),
                        period.endDate()
                );
        if (customerIds.isEmpty()) {
            return 0;
        }
        List<Customer> customers = customerRepository
                .findByIdInAndDeletedAtIsNullOrderByIdAsc(customerIds);
        if (customers.size() != customerIds.size()) {
            throw new IllegalStateException(
                    "月次請求対象に削除済みまたは存在しない顧客が含まれています。"
            );
        }

        for (Customer customer : customers) {
            String reportCode = invoiceReportCodeResolver.resolve(
                    customer.getInvoiceType()
            );

            executor.execute(
                    monthlyClosingId,
                    preview,
                    period,
                    closingVersion,
                    MonthlyClosingReportTarget.customer(
                            customer.getId(),
                            customer.getName()
                    ),
                    reportCode
            );
        }
        return customers.size();
    }
}
