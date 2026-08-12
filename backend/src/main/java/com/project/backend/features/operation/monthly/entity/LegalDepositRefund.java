package com.project.backend.features.operation.monthly.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.operation.monthly.enums.LegalDepositRefundStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "employee_legal_deposit_refund",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_legal_deposit_refund_version_employee",
                columnNames = {
                        "tenant_id", "monthly_closing_id",
                        "closing_version", "employee_id"
                }
        )
)
@Getter
@Setter
public class LegalDepositRefund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monthly_closing_id", nullable = false)
    private Long monthlyClosingId;

    @Column(name = "target_month", nullable = false)
    private LocalDate targetMonth;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "closing_version", nullable = false)
    private Integer closingVersion;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LegalDepositRefundStatus status = LegalDepositRefundStatus.ACTIVE;

    @Column(name = "superseded_at")
    private Instant supersededAt;
}
