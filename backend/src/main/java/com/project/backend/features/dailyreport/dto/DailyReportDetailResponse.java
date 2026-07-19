package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.project.backend.features.customer.enums.CustomerBillingUnit;
import com.project.backend.features.employee.enums.ApprovalStatus;

import lombok.Builder;

@Builder
public record DailyReportDetailResponse(
        Long id,

        Long employeeId,
        String employeeCode,
        String employeeName,

        LocalDate workDate,
        LocalDate paymentDate,

        Long customerId,
        Long customerSiteId,

        String customerName,
        String siteName,

        Long billingRateId,

        String jobCode,
        String jobName,

        String siteRoleCode,
        String siteRoleName,

        CustomerBillingUnit billingUnit,

        BigDecimal billingBaseUnitPrice,
        BigDecimal billingOvertimeUnitPrice,
        BigDecimal billingNightUnitPrice,
        BigDecimal billingHolidayUnitPrice,
        BigDecimal billingCommuteUnitPrice,

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

        BigDecimal estimatedGrossPayAmount,
        BigDecimal estimatedNetPayAmount,

        Boolean vehicleUsedFlag,
        BigDecimal mileage,

        BigDecimal paidLeaveDays,
        BigDecimal paidLeaveRemainingDays,
        BigDecimal paidLeaveRemainingAfterUsedDays,

        ApprovalStatus approvalStatus,
        String approvalComment,

        List<DailyReportAllowanceResponse> allowances,
        List<DailyReportDeductionResponse> deductions
) {
}