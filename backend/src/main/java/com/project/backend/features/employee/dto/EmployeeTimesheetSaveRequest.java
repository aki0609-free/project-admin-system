package com.project.backend.features.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.project.backend.features.employee.enums.ApprovalStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeTimesheetSaveRequest {
    private Long employeeId;
    private LocalDate workDate;
    private LocalTime clockIn;
    private LocalTime clockOut;
    private BigDecimal workHours = BigDecimal.ZERO;
    private BigDecimal overtimeHours = BigDecimal.ZERO;
    private BigDecimal nightShiftHours = BigDecimal.ZERO;
    private boolean weekendFlag = false;
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    private String approvalComment;
}