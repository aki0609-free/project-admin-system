package com.project.backend.features.system.report.invoice.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.report.invoice.entity.MonthlyInvoiceHistory;

public interface MonthlyInvoiceHistoryRepository
        extends JpaRepository<MonthlyInvoiceHistory, Long> {

    List<MonthlyInvoiceHistory>
            findByTargetMonthAndClosingVersionAndDeletedAtIsNullOrderByCustomerIdAsc(
                    LocalDate targetMonth,
                    Integer closingVersion
            );
}
