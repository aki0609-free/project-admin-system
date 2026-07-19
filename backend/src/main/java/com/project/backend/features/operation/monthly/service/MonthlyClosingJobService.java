package com.project.backend.features.operation.monthly.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.customer.entity.Customer;
import com.project.backend.features.customer.repository.CustomerRepository;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingPeriod;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingReportTarget;
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
    private final CustomerRepository customerRepository;
    private final MonthlyClosingJobExecutor executor;

    public void executeClosing(
            Long monthlyClosingId,
            MonthlyClosingPeriod period,
            Integer closingVersion
    ) {
        List<OperationReportPreview> previews =
                previewRepository
                        .findByOperationTypeAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                                OperationType.MONTHLY
                        );

        for (OperationReportPreview preview : previews) {
            String jobCode =
                    preview.getJobCode();

            if (jobCode == null || jobCode.isBlank()) {
                continue;
            }

            if (MONTHLY_INVOICE_JOB_CODE.equals(jobCode)) {
                executeMonthlyInvoice(
                        monthlyClosingId,
                        preview,
                        period,
                        closingVersion
                );

                continue;
            }

            executor.execute(
                    monthlyClosingId,
                    preview,
                    period,
                    closingVersion
            );
        }
    }

    private void executeMonthlyInvoice(
            Long monthlyClosingId,
            OperationReportPreview preview,
            MonthlyClosingPeriod period,
            Integer closingVersion
    ) {
        List<Customer> customers =
                customerRepository
                        .findByDeletedAtIsNullOrderByIdAsc();

        for (Customer customer : customers) {
            executor.execute(
                    monthlyClosingId,
                    preview,
                    period,
                    closingVersion,
                    MonthlyClosingReportTarget.customer(
                            customer.getId(),
                            customer.getName()
                    )
            );
        }
    }
}