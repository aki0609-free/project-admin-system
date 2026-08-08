package com.project.backend.features.system.report.invoice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 月次締めで確定した請求書ヘッダーの参照モデル。
 */
@Entity
@Immutable
@Table(name = "monthly_invoice_history")
@Getter
public class MonthlyInvoiceHistory extends BaseEntity {

    protected MonthlyInvoiceHistory() {
    }

    public MonthlyInvoiceHistory(
            Long id,
            LocalDate targetMonth,
            Integer closingVersion,
            Long customerId,
            BigDecimal totalAmount
    ) {
        this.id = id;
        this.targetMonth = targetMonth;
        this.closingVersion = closingVersion;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_month", nullable = false)
    private LocalDate targetMonth;

    @Column(name = "closing_version", nullable = false)
    private Integer closingVersion;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;
}
