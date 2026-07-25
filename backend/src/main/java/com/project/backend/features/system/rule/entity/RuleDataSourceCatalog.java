package com.project.backend.features.system.rule.entity;

import java.util.ArrayList;
import java.util.List;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "rule_data_source_catalog",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"tenant_id", "source_code"}
        )
)
@Getter
@Setter
public class RuleDataSourceCatalog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_code", nullable = false, length = 100)
    private String sourceCode;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "physical_name", nullable = false, length = 200)
    private String physicalName;

    @Column(name = "where_clause_template", length = 1000)
    private String whereClauseTemplate;

    @Column(name = "tenant_scoped_flag", nullable = false)
    private boolean tenantScopedFlag = true;

    @Column(name = "max_rows", nullable = false)
    private int maxRows = 1000;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    @OneToMany(
            mappedBy = "catalog",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<RuleDataSourceCatalogColumn> columns =
            new ArrayList<>();

    public void addColumn(RuleDataSourceCatalogColumn column) {
        column.setCatalog(this);
        columns.add(column);
    }
}
