package com.project.backend.features.system.settings.entity;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "system_setting_value",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_system_setting_value_tenant_definition",
                columnNames = {"tenant_id", "definition_id"}
        )
)
@Getter
@Setter
public class SystemSettingValue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id", nullable = false)
    private SystemSettingDefinition definition;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;
}
