package com.project.backend.features.operation.monthly.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingOutputType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "monthly_closing_output_definition",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_monthly_closing_output_definition",
                columnNames = {"tenant_id", "output_type", "output_code"}
        ),
        indexes = @Index(
                name = "idx_monthly_closing_output_active",
                columnList = "tenant_id,active_flag,execution_order"
        )
)
@Getter
@Setter
public class MonthlyClosingOutputDefinition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_type", nullable = false, length = 30)
    private MonthlyClosingOutputType outputType;

    @Column(name = "output_code", nullable = false, length = 100)
    private String outputCode;

    @Column(name = "execution_order", nullable = false)
    private Integer executionOrder = 1;

    @Column(name = "required_flag", nullable = false)
    private Boolean requiredFlag = true;

    @Column(name = "active_flag", nullable = false)
    private Boolean activeFlag = true;

    @Column(name = "backup_retention_years")
    private Integer backupRetentionYears;
}
