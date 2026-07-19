package com.project.backend.features.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.project.backend.features.employee.enums.ApprovalStatus;

import lombok.Builder;

@Builder
public record EmployeeLoanResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        BigDecimal principal,
        BigDecimal currentBalance,
        BigDecimal monthlyRepayment,
        LocalDate loanDate,
        LocalDate repaymentStartDate,
        boolean activeFlag,
        ApprovalStatus approvalStatus,
        String approvalComment
) {
}