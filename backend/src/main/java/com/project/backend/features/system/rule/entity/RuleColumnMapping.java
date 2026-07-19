package com.project.backend.features.system.rule.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.rule.enums.RuleDataType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rule_column_mapping")
@Getter
@Setter
public class RuleColumnMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "data_source_id", nullable = false)
    private RuleDataSource dataSource;

    @Column(name = "column_name", nullable = false, length = 200)
    private String columnName;

    /**
     * dataSource内でのキー。
     * 例: hourlyWage
     * employee.hourlyWage のようなドット指定はDSL側で sourceName + factKey で参照する。
     */
    @Column(name = "fact_key", nullable = false, length = 200)
    private String factKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private RuleDataType dataType = RuleDataType.STRING;

    @Column(name = "required_flag", nullable = false)
    private boolean requiredFlag = false;

    @Column(name = "order_no", nullable = false)
    private int orderNo = 1;
}