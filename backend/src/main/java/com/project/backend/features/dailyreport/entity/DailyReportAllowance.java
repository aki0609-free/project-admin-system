package com.project.backend.features.dailyreport.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "daily_report_allowances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyReportAllowance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "daily_report_id", nullable = false)
    private Long dailyReportId;

    @Column(name = "allowance_master_id", nullable = false)
    private Long allowanceMasterId;

    @Column(name = "allowance_code", nullable = false)
    private String allowanceCode;

    @Column(name = "allowance_name", nullable = false)
    private String allowanceName;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "calculated_amount", nullable = false)
    private Integer calculatedAmount;

    @Column(name = "manual_override_flag", nullable = false)
    private boolean manualOverrideFlag;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "quantity", precision = 12, scale = 2)
    private java.math.BigDecimal quantity;

    @Column(name = "balance_unit", length = 20)
    private String balanceUnit;
}
