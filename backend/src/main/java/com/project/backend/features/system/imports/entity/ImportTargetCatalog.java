package com.project.backend.features.system.imports.entity;

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
        name = "import_target_catalog",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"tenant_id", "table_name"}
        )
)
@Getter
@Setter
public class ImportTargetCatalog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 200)
    private String tableName;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "tenant_scoped_flag", nullable = false)
    private boolean tenantScopedFlag = true;

    @Column(name = "allow_delete_insert_flag", nullable = false)
    private boolean allowDeleteInsertFlag;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    @OneToMany(
            mappedBy = "catalog",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ImportTargetCatalogColumn> columns =
            new ArrayList<>();

    public void addColumn(ImportTargetCatalogColumn column) {
        column.setCatalog(this);
        columns.add(column);
    }
}
