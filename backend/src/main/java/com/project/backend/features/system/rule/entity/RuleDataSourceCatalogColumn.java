package com.project.backend.features.system.rule.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.system.rule.enums.RuleDataType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "rule_data_source_catalog_column",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "tenant_id",
                        "catalog_id",
                        "column_name"
                }
        )
)
@Getter
@Setter
public class RuleDataSourceCatalogColumn extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_id", nullable = false)
    private RuleDataSourceCatalog catalog;

    @Column(name = "column_name", nullable = false, length = 200)
    private String columnName;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 50)
    private RuleDataType dataType = RuleDataType.STRING;

    @Column(name = "order_no", nullable = false)
    private int orderNo = 1;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}
