package com.project.backend.features.master.deduction.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.master.deduction.enums.DeductionCalculationType;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;
import com.project.backend.features.master.deduction.enums.DeductionType;
import com.project.backend.features.master.deduction.enums.DeductionUnit;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deduction_masters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeductionMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deduction_code", nullable = false, unique = true)
    private String deductionCode;

    @Column(name = "deduction_name", nullable = false)
    private String deductionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "deduction_type")
    private DeductionType deductionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type")
    private DeductionCalculationType calculationType;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "deduction_unit")
    private DeductionUnit deductionUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "detail_view_type")
    private DeductionDetailViewType detailViewType;

    @Column(name = "show_on_daily_statement")
    private Boolean showOnDailyStatement;

    @Column(name = "show_on_monthly_statement")
    private Boolean showOnMonthlyStatement;

    @Column(name = "carry_to_monthly_settlement")
    private Boolean carryToMonthlySettlement;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "note")
    private String note;
}