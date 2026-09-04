package com.project.backend.features.master.payrollitem.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "employee_payroll_item_transaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_employee_payroll_item_transaction_source",
                columnNames = {
                        "tenant_id", "employee_id", "target_type",
                        "target_code", "source_reference"
                }
        )
)
@Getter
@Setter
public class EmployeePayrollItemTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private PayrollItemTargetType targetType;

    @Column(name = "target_master_id", nullable = false)
    private Long targetMasterId;

    @Column(name = "target_code", nullable = false, length = 50)
    private String targetCode;

    @Column(name = "target_name", nullable = false, length = 200)
    private String targetName;

    @Column(name = "target_month", nullable = false)
    private LocalDate targetMonth;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "quantity", precision = 12, scale = 2)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_purpose", nullable = false, length = 30)
    private PayrollItemTransactionPurpose transactionPurpose =
            PayrollItemTransactionPurpose.PAYROLL_ITEM;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_effect", nullable = false, length = 20)
    private PayrollItemBalanceEffect balanceEffect = PayrollItemBalanceEffect.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private PayrollItemTransactionSource sourceType;

    @Column(name = "source_reference", length = 150)
    private String sourceReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayrollItemTransactionStatus status;

    @Column(name = "note", length = 500)
    private String note;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;
}
