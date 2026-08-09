package com.project.backend.features.system.report.invoice.entity;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "closing_version", nullable = false)
    private Integer closingVersion;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "invoice_type", nullable = false, length = 30)
    private String invoiceType;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "company_postal_code", length = 20)
    private String companyPostalCode;

    @Column(name = "company_address", length = 1000)
    private String companyAddress;

    @Column(name = "company_phone", length = 50)
    private String companyPhone;

    @Column(name = "company_fax", length = 50)
    private String companyFax;

    @Column(name = "qualified_invoice_issuer_number", length = 50)
    private String qualifiedInvoiceIssuerNumber;

    @Column(name = "bank_display_text", length = 1000)
    private String bankDisplayText;

    @Column(name = "invoice_note", columnDefinition = "text")
    private String invoiceNote;

    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "subtotal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "business_key", nullable = false)
    private String businessKey;

    @Column(name = "source_execution_id", nullable = false, length = 100)
    private String sourceExecutionId;

    @Column(name = "fixed_at", nullable = false)
    private Instant fixedAt;
}
