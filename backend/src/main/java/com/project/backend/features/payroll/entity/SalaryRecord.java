package com.project.backend.features.payroll.entity;

import java.math.BigDecimal;
import java.time.YearMonth;

import com.project.backend.app.base.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "salary_records")
public class SalaryRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeeId;

    @Column(nullable = false, name = "ym")
    private YearMonth yearMonth;

    @Column(precision = 12, scale = 0)
    private BigDecimal grossSalary;
    
}
