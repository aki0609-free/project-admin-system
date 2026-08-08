package com.project.backend.features.customer.service;

import org.springframework.stereotype.Component;

import com.project.backend.features.customer.repository.CustomerTransactionRepository;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationAssignmentRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationDispatchRepository;
import com.project.backend.features.system.report.invoice.repository.MonthlyInvoiceHistoryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerReferenceGuard {

    private final DailyReportRepository dailyReportRepository;
    private final DailyPreparationAssignmentRepository assignmentRepository;
    private final DailyPreparationDispatchRepository dispatchRepository;
    private final CustomerTransactionRepository transactionRepository;
    private final MonthlyInvoiceHistoryRepository invoiceHistoryRepository;

    public void assertCustomerDeletable(Long customerId) {
        if (dailyReportRepository.existsByCustomerIdAndDeletedAtIsNull(customerId)) {
            throw referenced("日報");
        }
        if (assignmentRepository.existsByCustomerIdAndDeletedAtIsNull(customerId)
                || dispatchRepository.existsByCustomerIdAndDeletedAtIsNull(customerId)) {
            throw referenced("翌日準備");
        }
        if (transactionRepository.existsByCustomerIdAndDeletedAtIsNull(customerId)) {
            throw referenced("取引情報");
        }
        if (invoiceHistoryRepository.existsByCustomerIdAndDeletedAtIsNull(customerId)) {
            throw referenced("請求履歴");
        }
    }

    public void assertSiteDeletable(Long customerSiteId) {
        if (dailyReportRepository.existsByCustomerSiteIdAndDeletedAtIsNull(customerSiteId)) {
            throw siteReferenced("日報");
        }
        if (assignmentRepository.existsByCustomerSiteIdAndDeletedAtIsNull(customerSiteId)
                || dispatchRepository.existsByCustomerSiteIdAndDeletedAtIsNull(customerSiteId)) {
            throw siteReferenced("翌日準備");
        }
    }

    private IllegalStateException referenced(String source) {
        return new IllegalStateException(
                source + "から参照されている顧客は削除できません。"
        );
    }

    private IllegalStateException siteReferenced(String source) {
        return new IllegalStateException(
                source + "から参照されている現場は削除できません。"
        );
    }
}
