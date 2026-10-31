package com.project.backend.features.admin.business.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.employee.entity.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee_year_end_adjustment")
@Getter
@Setter
public class EmployeeYearEndAdjustment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "tax_year", nullable = false)
    private Integer taxYear;

    @Column(name = "target_month", nullable = false)
    private LocalDate targetMonth;

    @Column(name = "adjustment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "note", length = 500)
    private String note;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion = 0L;
}
