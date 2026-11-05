package com.project.backend.features.system.settings.entity;

import java.time.Instant;

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
        name = "system_setting_definition",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_system_setting_definition_key",
                columnNames = {
                    "setting_scope", "owner_tenant_id",
                    "large_category_key", "middle_category_key", "small_category_key"
                }
        )
)
@Getter
@Setter
public class SystemSettingDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "large_category_key", nullable = false, length = 100)
    private String largeCategoryKey;

    @Column(name = "large_category_name", nullable = false, length = 200)
    private String largeCategoryName;

    @Column(name = "middle_category_key", nullable = false, length = 100)
    private String middleCategoryKey;

    @Column(name = "middle_category_name", nullable = false, length = 200)
    private String middleCategoryName;

    @Column(name = "small_category_key", nullable = false, length = 100)
    private String smallCategoryKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "setting_scope", nullable = false, length = 20)
    private SystemSettingScope scope;

    @Column(name = "owner_tenant_id", nullable = false, length = 255)
    private String ownerTenantId;

    @Column(name = "setting_name", nullable = false, length = 200)
    private String settingName;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 30)
    private SystemSettingValueType valueType;

    @Column(name = "required_flag", nullable = false)
    private boolean requiredFlag;

    @Column(name = "validation_pattern", length = 1000)
    private String validationPattern;

    @Column(name = "placeholder", length = 500)
    private String placeholder;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "sensitive_flag", nullable = false)
    private boolean sensitiveFlag;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag;

    @Column(name = "system_managed_flag", nullable = false)
    private boolean systemManagedFlag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
