package com.project.backend.features.customer.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.customer.enums.CustomerBillingUnit;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "customer_site_billing_rates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_customer_site_billing_rate",
                        columnNames = {
                                "tenant_id",
                                "customer_site_id",
                                "job_code",
                                "site_role_code",
                                "effective_from"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_customer_site_billing_rate_lookup",
                        columnList =
                                "tenant_id, customer_site_id, job_code, "
                                        + "site_role_code, effective_from, effective_to"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSiteBillingRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_site_id", nullable = false)
    private CustomerSite customerSite;

    @Column(name = "job_code", nullable = false, length = 100)
    private String jobCode;

    @Column(name = "job_name", nullable = false, length = 200)
    private String jobName;

    /**
     * 役職なしの場合も GENERAL 等のコードを入れる。
     */
    @Column(name = "site_role_code", nullable = false, length = 100)
    private String siteRoleCode;

    @Column(name = "site_role_name", nullable = false, length = 200)
    private String siteRoleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_unit", nullable = false, length = 30)
    @Builder.Default
    private CustomerBillingUnit billingUnit = CustomerBillingUnit.DAILY;

    /**
     * DAILYなら日額、HOURLYなら時間単価、
     * MONTHLYなら月額、FIXEDなら固定額。
     */
    @Column(
            name = "base_unit_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal baseUnitPrice = BigDecimal.ZERO;

    /**
     * 残業時間に対して適用する請求単価。
     * 完成単価か加算単価かは、月次請求処理側のルールで決定する。
     */
    @Column(
            name = "overtime_unit_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal overtimeUnitPrice = BigDecimal.ZERO;

    /**
     * 深夜時間に対して適用する請求単価。
     * 完成単価か加算単価かは、月次請求処理側のルールで決定する。
     */
    @Column(
            name = "night_unit_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal nightUnitPrice = BigDecimal.ZERO;

    /**
     * 休日労働時間に対して適用する請求単価。
     * 完成単価か加算単価かは、月次請求処理側のルールで決定する。
     */
    @Column(
            name = "holiday_unit_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal holidayUnitPrice = BigDecimal.ZERO;

    /**
     * 走行距離1kmあたりの請求単価。
     * 月次請求では mileage × commuteUnitPrice で計算する。
     */
    @Column(
            name = "commute_unit_price",
            nullable = false,
            precision = 15,
            scale = 2
    )
    @Builder.Default
    private BigDecimal commuteUnitPrice = BigDecimal.ZERO;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;

    @Column(name = "active_flag", nullable = false)
    @Builder.Default
    private Boolean activeFlag = true;

    @Column(name = "note", length = 1000)
    private String note;
}