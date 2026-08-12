package com.project.backend.features.operation.monthly.entity;

import java.time.Instant;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;

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
        name = "customer_billing_closings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customer_billing_closing_customer",
                columnNames = {"tenant_id", "target_month", "customer_id"}
        )
)
@Getter
@Setter
public class CustomerBillingClosing extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_month", nullable = false)
    private LocalDate targetMonth;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MonthlyClosingStatus status = MonthlyClosingStatus.OPEN;

    @Column(name = "closing_version", nullable = false)
    private Integer closingVersion = 0;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by", length = 100)
    private String closedBy;
}
