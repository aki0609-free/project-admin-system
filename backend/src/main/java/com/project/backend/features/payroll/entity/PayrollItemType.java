package com.project.backend.features.payroll.entity;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.payroll.enums.ItemCategory;
import com.project.backend.features.tax.enums.CalcType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payroll_item_types")
public class PayrollItemType extends BaseEntity {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String nameJa;

    @Column(nullable = false)
    private String nameEn;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalcType calcType;

    @Column(name = "is_active")
    private Boolean isActive = true;

}
