package com.project.backend.features.admin.business.entity;

import java.math.BigDecimal;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.employee.enums.DormitoryType;

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
        name = "dormitory_fee_setting",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dormitory_fee_setting_type",
                columnNames = {"tenant_id", "dormitory_type"}
        )
)
@Getter
@Setter
public class DormitoryFeeSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dormitory_type", nullable = false, length = 30)
    private DormitoryType dormitoryType;

    @Column(name = "daily_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyAmount = BigDecimal.ZERO;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}
