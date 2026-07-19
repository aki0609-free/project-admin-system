package com.project.backend.features.system.rule.entity;

import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.rule.enums.RuleDslType;
import com.project.backend.features.system.rule.enums.RuleType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "rule_master",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "rule_name"})
        }
)
@Getter
@Setter
public class RuleMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    @Column(name = "rule_display_name", nullable = false, length = 200)
    private String ruleDisplayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType = RuleType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "dsl_type", nullable = false, length = 50)
    private RuleDslType dslType = RuleDslType.MVEL;

    @Lob
    @Column(name = "dsl_text")
    private String dslText;

    /**
     * JAVA_BEAN を使う場合だけ指定。
     * 通常の allowance / deduction では使わない。
     */
    @Column(name = "rule_bean_name", length = 200)
    private String ruleBeanName;

    @Column(name = "result_fact_key", nullable = false, length = 100)
    private String resultFactKey = "result";

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "priority", nullable = false)
    private int priority = 100;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuleParameter> parameters = new ArrayList<>();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuleDataSource> dataSources = new ArrayList<>();

    public void addParameter(RuleParameter parameter) {
        parameter.setRule(this);
        this.parameters.add(parameter);
    }

    public void clearParameters() {
        this.parameters.clear();
    }

    public void addDataSource(RuleDataSource dataSource) {
        dataSource.setRule(this);
        this.dataSources.add(dataSource);
    }

    public void clearDataSources() {
        this.dataSources.clear();
    }
}