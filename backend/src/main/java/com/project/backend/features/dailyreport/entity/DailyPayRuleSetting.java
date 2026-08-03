package com.project.backend.features.dailyreport.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.dailyreport.enums.DailyPayComponentType;

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

/**
 * 日報給与内訳とRuleの対応設定。
 *
 * <p>計算式はRule管理へ置き、このテーブルは用途との対応だけを管理する。</p>
 */
@Entity
@Table(
        name = "daily_pay_rule_setting",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"tenant_id", "component_type"}
        )
)
@Getter
@Setter
public class DailyPayRuleSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 30)
    private DailyPayComponentType componentType;

    @Column(name = "rule_name", nullable = false, length = 150)
    private String ruleName;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}
