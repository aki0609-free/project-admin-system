package com.project.backend.features.customer.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.customer.enums.CustomerInvoiceType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "furigana_name")
    private String furiganaName;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "post_no")
    private String postNo;

    private String address;

    @Column(name = "representative_name")
    private String representativeName;

    private String phone;

    @Column(name = "job_type")
    private String jobType;

    @Column(name = "contract_flag")
    private String contractFlag;

    /**
     * 顧客に適用する請求書レイアウト。
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "invoice_type",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private CustomerInvoiceType invoiceType =
            CustomerInvoiceType.PATTERN_1;

    @Enumerated(EnumType.STRING)
    @Column(name = "closing_day_type")
    private DayRuleType closingDayType;

    @Column(name = "closing_day_value")
    private Integer closingDayValue;

    @Column(
            name = "closing_month_offset",
            nullable = false
    )
    @Builder.Default
    private Integer closingMonthOffset = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_day_type")
    private DayRuleType paymentDayType;

    @Column(name = "payment_day_value")
    private Integer paymentDayValue;

    @Column(
            name = "payment_month_offset",
            nullable = false
    )
    @Builder.Default
    private Integer paymentMonthOffset = 0;
}