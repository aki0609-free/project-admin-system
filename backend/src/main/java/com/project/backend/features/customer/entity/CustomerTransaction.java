package com.project.backend.features.customer.entity;

import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.customer.enums.CustomerPaymentStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "target_month", nullable = false, length = 7)
    private String targetMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "closing_day_type")
    private DayRuleType closingDayType;

    @Column(name = "closing_day_value")
    private Integer closingDayValue;

    @Column(name = "closing_month_offset", nullable = false)
    private Integer closingMonthOffset;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_day_type")
    private DayRuleType paymentDayType;

    @Column(name = "payment_day_value")
    private Integer paymentDayValue;

    @Column(name = "payment_month_offset", nullable = false)
    private Integer paymentMonthOffset;

    @Column(name = "billing_amount")
    private Integer billingAmount;

    @Column(name = "expected_payment_date")
    private LocalDate expectedPaymentDate;

    @Column(name = "confirmed_payment_date")
    private LocalDate confirmedPaymentDate;

    @Column(name = "paid_amount")
    private Integer paidAmount;

    @Column(name = "offset_amount")
    private Integer offsetAmount;

    @Column(name = "fee")
    private Integer fee;

    @Column(name = "total_amount")
    private Integer totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 30)
    private CustomerPaymentStatus paymentStatus;

    @Column(name = "note")
    private String note;
}