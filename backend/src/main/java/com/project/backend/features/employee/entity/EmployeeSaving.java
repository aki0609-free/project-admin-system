package com.project.backend.features.employee.entity;

import java.math.BigDecimal;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.employee.enums.ApprovalStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee_saving")
@Getter
@Setter
public class EmployeeSaving extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage = BigDecimal.ZERO;

    @Column(name = "min_salary_threshold", precision = 12, scale = 2)
    private BigDecimal minSalaryThreshold = BigDecimal.ZERO;

    @Column(name = "current_balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "approval_comment", length = 1000)
    private String approvalComment;
}