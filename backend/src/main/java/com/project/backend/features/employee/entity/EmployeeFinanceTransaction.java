package com.project.backend.features.employee.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.employee.enums.EmployeeFinanceAccountType;
import com.project.backend.features.employee.enums.EmployeeFinanceTransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "employee_finance_transaction",
        indexes = {
                @Index(
                        name = "idx_employee_finance_transaction_employee",
                        columnList = "tenant_id, employee_id, transaction_date"
                ),
                @Index(
                        name = "idx_employee_finance_transaction_source",
                        columnList = "tenant_id, daily_report_id"
                )
        }
)
@Getter
@Setter
public class EmployeeFinanceTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private EmployeeFinanceAccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private EmployeeFinanceTransactionType transactionType;

    @Column(name = "account_reference_id", nullable = false)
    private Long accountReferenceId;

    @Column(name = "daily_report_id")
    private Long dailyReportId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /** 残高に対する符号付き増減額。 */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "balance_before", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceBefore = BigDecimal.ZERO;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    @Column(name = "note", length = 500)
    private String note;
}
