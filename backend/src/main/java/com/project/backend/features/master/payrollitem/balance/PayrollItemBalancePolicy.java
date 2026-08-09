package com.project.backend.features.master.payrollitem.balance;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

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

@Entity
@Table(name = "payroll_item_balance_policy")
@Getter
@Setter
public class PayrollItemBalancePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private PayrollItemTargetType targetType;

    @Column(name = "target_master_id", nullable = false)
    private Long targetMasterId;

    @Column(name = "target_code", nullable = false, length = 50)
    private String targetCode;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_unit", nullable = false, length = 20)
    private BalanceUnit balanceUnit;

    @Column(name = "accrual_frequency", nullable = false, length = 20)
    private String accrualFrequency;

    @Column(name = "accrual_rule_name", nullable = false, length = 100)
    private String accrualRuleName;

    @Column(name = "carry_forward_flag", nullable = false)
    private boolean carryForwardFlag;

    @Column(name = "advance_consumption_flag", nullable = false)
    private boolean advanceConsumptionFlag;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag;
}
