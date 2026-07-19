package com.project.backend.features.employee.dto;

import java.math.BigDecimal;

import com.project.backend.features.employee.enums.ApprovalStatus;

import lombok.Builder;

@Builder
public record EmployeeSavingResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        BigDecimal percentage,
        BigDecimal minSalaryThreshold,
        BigDecimal currentBalance,
        boolean activeFlag,
        ApprovalStatus approvalStatus,
        String approvalComment
) {
}