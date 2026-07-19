package com.project.backend.features.system.rule.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.rule.enums.RuleDataType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rule_parameter")
@Getter
@Setter
public class RuleParameter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private RuleMaster rule;

    @Column(name = "param_name", nullable = false, length = 150)
    private String paramName;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private RuleDataType dataType = RuleDataType.STRING;

    @Column(name = "required_flag", nullable = false)
    private boolean requiredFlag = false;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "order_no", nullable = false)
    private int orderNo = 1;
}