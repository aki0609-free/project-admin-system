package com.project.backend.features.master.allowance.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.master.allowance.enums.AllowanceCalculationType;
import com.project.backend.features.master.allowance.enums.AllowanceDetailViewType;
import com.project.backend.features.master.allowance.enums.AllowanceType;
import com.project.backend.features.master.allowance.enums.AllowanceUnit;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "allowance_masters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowanceMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allowance_code", nullable = false, unique = true)
    private String allowanceCode;

    @Column(name = "allowance_name", nullable = false)
    private String allowanceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "allowance_type")
    private AllowanceType allowanceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type")
    private AllowanceCalculationType calculationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "allowance_unit")
    private AllowanceUnit allowanceUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "detail_view_type")
    private AllowanceDetailViewType detailViewType;

    @Column(name = "rule_name", length = 150)
    private String ruleName;

    @Column(name = "default_amount")
    private Integer defaultAmount;

    @Column(name = "allow_manual_input")
    private Boolean allowManualInput;

    @Column(name = "min_amount")
    private Integer minAmount;

    @Column(name = "max_amount")
    private Integer maxAmount;

    @Column(name = "taxable")
    private Boolean taxable;

    @Column(name = "show_on_daily_statement")
    private Boolean showOnDailyStatement;

    @Column(name = "show_on_monthly_statement")
    private Boolean showOnMonthlyStatement;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "note")
    private String note;
}