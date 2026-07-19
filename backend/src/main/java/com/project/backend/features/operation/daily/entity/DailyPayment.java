package com.project.backend.features.operation.daily.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.operation.daily.enums.DailyPaymentStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "daily_payments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "payment_date", "employee_id"})
        }
)
@Getter
@Setter
public class DailyPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_code", length = 100)
    private String employeeCode;

    @Column(name = "employee_name", length = 200)
    private String employeeName;

    @Column(name = "planned_amount", precision = 12, scale = 2)
    private BigDecimal plannedAmount = BigDecimal.ZERO;

    @Column(name = "actual_amount", precision = 12, scale = 2)
    private BigDecimal actualAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DailyPaymentStatus status = DailyPaymentStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "note", length = 1000)
    private String note;
}