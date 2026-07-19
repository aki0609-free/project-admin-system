package com.project.backend.features.employee.dto;

import java.math.BigDecimal;

import com.project.backend.features.employee.enums.ApprovalStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeSavingSaveRequest {

    private Long employeeId;

    private BigDecimal percentage = BigDecimal.ZERO;

    private BigDecimal minSalaryThreshold = BigDecimal.ZERO;

    private BigDecimal currentBalance = BigDecimal.ZERO;

    private boolean activeFlag = true;

    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    private String approvalComment;
}