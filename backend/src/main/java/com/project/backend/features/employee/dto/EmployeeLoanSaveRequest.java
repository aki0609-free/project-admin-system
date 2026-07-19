package com.project.backend.features.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.features.employee.enums.ApprovalStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeLoanSaveRequest {

    private Long employeeId;

    private BigDecimal principal = BigDecimal.ZERO;

    private BigDecimal currentBalance = BigDecimal.ZERO;

    private BigDecimal monthlyRepayment = BigDecimal.ZERO;

    private LocalDate loanDate;

    private LocalDate repaymentStartDate;

    private boolean activeFlag = true;

    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    private String approvalComment;
}