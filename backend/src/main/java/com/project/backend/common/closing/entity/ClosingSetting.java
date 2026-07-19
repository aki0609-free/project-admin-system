package com.project.backend.common.closing.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.common.dayrule.enums.DayRuleType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "closing_setting")
@Getter
@Setter
public class ClosingSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_code", nullable = false, length = 50)
    private String settingCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "closing_day_type", nullable = false, length = 30)
    private DayRuleType closingDayType;

    @Column(name = "closing_day_value")
    private Integer closingDayValue;

    @Column(name = "closing_month_offset", nullable = false)
    private Integer closingMonthOffset = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_day_type", length = 30)
    private DayRuleType paymentDayType;

    @Column(name = "payment_day_value")
    private Integer paymentDayValue;

    @Column(name = "payment_month_offset", nullable = false)
    private Integer paymentMonthOffset = 0;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}