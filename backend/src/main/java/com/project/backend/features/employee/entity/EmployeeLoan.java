package com.project.backend.features.employee.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.app.base.entity.BaseEntity;
import com.project.backend.features.employee.enums.ApprovalStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employee_loan")
@Getter
@Setter
public class EmployeeLoan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "principal", nullable = false, precision = 12, scale = 2)
    private BigDecimal principal = BigDecimal.ZERO;

    @Column(name = "monthly_repayment", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRepayment = BigDecimal.ZERO;

    @Column(name = "loan_date")
    private LocalDate loanDate;

    @Column(name = "current_balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "repayment_start_date")
    private LocalDate repaymentStartDate;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "approval_comment", length = 1000)
    private String approvalComment;
}