package com.project.backend.features.payroll.entity;

import java.math.BigDecimal;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payslip_items")
public class PayslipItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long payrollItemTypeId;

    @Column(precision = 12, scale = 0, nullable = false)
    private BigDecimal amount;

    @Column(length = 100)
    private String description;

    @ManyToOne
    @JoinColumn(name = "payslip_id", nullable = false)
    private Payslip payslip;
    
}
