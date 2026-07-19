package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.project.backend.features.employee.enums.ApprovalStatus;

public record DailyReportSaveRequest(

        Long employeeId,

        LocalDate workDate,
        LocalDate paymentDate,

        Long customerId,
        Long customerSiteId,

        String customerName,
        String siteName,

        String jobCode,
        String jobName,

        String siteRoleCode,
        String siteRoleName,

        String workDescription,

        LocalTime startTime,
        LocalTime endTime,

        Integer breakMinutes,

        BigDecimal workHours,
        BigDecimal overtimeHours,
        BigDecimal nightWorkHours,
        BigDecimal holidayWorkHours,

        BigDecimal allowanceAmount,
        BigDecimal deductionAmount,

        BigDecimal loanRepaymentAmount,
        BigDecimal savingAmount,

        Boolean vehicleUsedFlag,
        BigDecimal mileage,

        BigDecimal paidLeaveDays,

        ApprovalStatus approvalStatus,
        String approvalComment,

        List<DailyReportAllowanceSaveRequest> allowances,
        List<DailyReportDeductionSaveRequest> deductions

) {
    public DailyReportSaveRequest {
        allowances = allowances == null
                ? List.of()
                : List.copyOf(allowances);

        deductions = deductions == null
                ? List.of()
                : List.copyOf(deductions);
    }
}