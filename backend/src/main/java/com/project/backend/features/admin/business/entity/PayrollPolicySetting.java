package com.project.backend.features.admin.business.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 会社（テナント）ごとの期間付き給与計算制度。 */
@Entity
@Table(name = "payroll_policy_setting")
@Getter
@Setter
public class PayrollPolicySetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "week_start_day", nullable = false, length = 20)
    private DayOfWeek weekStartDay = DayOfWeek.MONDAY;

    @Column(name = "weekly_statutory_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal weeklyStatutoryHours = new BigDecimal("40.00");

    @Column(name = "monthly_overtime_threshold_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal monthlyOvertimeThresholdHours = new BigDecimal("60.00");

    @Column(name = "overtime_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal overtimeRate = new BigDecimal("1.250");

    @Column(name = "overtime_over_threshold_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal overtimeOverThresholdRate = new BigDecimal("1.500");

    @Column(name = "night_premium_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal nightPremiumRate = new BigDecimal("0.250");

    @Column(name = "statutory_holiday_rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal statutoryHolidayRate = new BigDecimal("1.350");

    @Column(name = "daily_standard_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal dailyStandardHours = new BigDecimal("8.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_rounding_mode", nullable = false, length = 20)
    private RoundingMode amountRoundingMode = RoundingMode.HALF_UP;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}
