package com.project.backend.features.master.payrollitem.balance;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "payroll_item_parameter_definition",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payroll_item_parameter_definition_key",
                columnNames = {"tenant_id", "balance_policy_id", "parameter_key"}
        )
)
@Getter
@Setter
public class PayrollItemParameterDefinition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "balance_policy_id", nullable = false)
    private Long balancePolicyId;

    @Column(name = "parameter_key", nullable = false, length = 100)
    private String parameterKey;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 20)
    private PayrollItemParameterInputType inputType;

    @Column(name = "required_flag", nullable = false)
    private boolean requiredFlag;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "options_json", columnDefinition = "json")
    private String optionsJson;

    @Column(name = "rule_parameter_flag", nullable = false)
    private boolean ruleParameterFlag;

    @Column(name = "daily_display_flag", nullable = false)
    private boolean dailyDisplayFlag;

    @Column(name = "input_source_override_flag", nullable = false)
    private boolean inputSourceOverrideFlag;

    @Column(name = "rule_value_resolver_key", length = 100)
    private String ruleValueResolverKey;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}
