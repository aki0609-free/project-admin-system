package com.project.backend.features.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.project.backend.features.employee.enums.ApprovalStatus;

import lombok.Builder;

@Builder
public record EmployeeTimesheetResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        LocalDate workDate,
        LocalTime clockIn,
        LocalTime clockOut,
        BigDecimal workHours,
        BigDecimal overtimeHours,
        BigDecimal nightShiftHours,
        boolean weekendFlag,
        ApprovalStatus approvalStatus,
        String approvalComment
) {
}