package com.project.backend.features.system.masterdata.entity;

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
        name = "generic_master_definition",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_generic_master_definition_code",
                columnNames = {"tenant_id", "master_code"}
        )
)
@Getter
@Setter
public class GenericMasterDefinition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "master_code", nullable = false, length = 100)
    private String masterCode;

    @Column(name = "master_name", nullable = false, length = 200)
    private String masterName;

    @Column(name = "tab_name", nullable = false, length = 100)
    private String tabName;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement", nullable = false, length = 40)
    private GenericMasterPlacement placement;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "definition_status", nullable = false, length = 20)
    private GenericMasterStatus status;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag;
}
