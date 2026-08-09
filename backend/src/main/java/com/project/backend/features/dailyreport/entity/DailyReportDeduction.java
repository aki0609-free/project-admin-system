package com.project.backend.features.dailyreport.entity;

import java.math.BigDecimal;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_report_deductions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyReportDeduction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "daily_report_id", nullable = false)
    private Long dailyReportId;

    @Column(name = "deduction_master_id", nullable = false)
    private Long deductionMasterId;

    @Column(name = "deduction_code", nullable = false)
    private String deductionCode;

    @Column(name = "deduction_name", nullable = false)
    private String deductionName;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "calculated_amount", nullable = false)
    private Integer calculatedAmount;

    @Column(name = "manual_override_flag", nullable = false)
    private boolean manualOverrideFlag;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "quantity", precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(name = "balance_unit", length = 20)
    private String balanceUnit;
}
