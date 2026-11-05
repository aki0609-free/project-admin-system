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
        name = "generic_master_field_definition",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_generic_master_field_key",
                columnNames = {"tenant_id", "definition_id", "field_key"}
        )
)
@Getter
@Setter
public class GenericMasterFieldDefinition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_id", nullable = false)
    private Long definitionId;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Column(name = "field_label", nullable = false, length = 200)
    private String fieldLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 30)
    private GenericMasterFieldType fieldType;

    @Column(name = "required_flag", nullable = false)
    private boolean requiredFlag;

    @Column(name = "list_visible_flag", nullable = false)
    private boolean listVisibleFlag;

    @Column(name = "searchable_flag", nullable = false)
    private boolean searchableFlag;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "placeholder", length = 500)
    private String placeholder;

    @Column(name = "validation_pattern", length = 1000)
    private String validationPattern;

    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;
}
