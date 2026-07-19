package com.project.backend.features.dailyreport.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.project.backend.features.dailyreport.dto.DailyReportResponse;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.enums.ApprovalStatus;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DailyReportMapper {

    List<DailyReportResponse> toResponseList(
            List<DailyReport> entities
    );

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeCode", source = "employee.employeeCode")
    @Mapping(target = "employeeName", source = "employee.employeeName")
    @Mapping(target = "paidLeaveRemainingDays", ignore = true)
    @Mapping(target = "paidLeaveRemainingAfterUsedDays", ignore = true)
    DailyReportResponse toResponse(DailyReport entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)

    /*
     * 請求単価スナップショットはクライアントから更新しない。
     */
    @Mapping(target = "billingRateId", ignore = true)
    @Mapping(target = "billingUnit", ignore = true)
    @Mapping(target = "billingBaseUnitPrice", ignore = true)
    @Mapping(target = "billingOvertimeUnitPrice", ignore = true)
    @Mapping(target = "billingNightUnitPrice", ignore = true)
    @Mapping(target = "billingHolidayUnitPrice", ignore = true)
    @Mapping(target = "billingCommuteUnitPrice", ignore = true)

    @Mapping(target = "estimatedGrossPayAmount", ignore = true)
    @Mapping(target = "estimatedNetPayAmount", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromRequest(
            DailyReportSaveRequest request,
            @MappingTarget DailyReport entity
    );

    default void applyRequest(
            DailyReportSaveRequest request,
            DailyReport entity,
            Employee employee
    ) {
        entity.setEmployee(employee);
        updateEntityFromRequest(request, entity);
    }

    @AfterMapping
    default void afterUpdateEntityFromRequest(
            DailyReportSaveRequest request,
            @MappingTarget DailyReport entity
    ) {
        if (request == null) {
            entity.setBreakMinutes(0);

            entity.setWorkHours(BigDecimal.ZERO);
            entity.setOvertimeHours(BigDecimal.ZERO);
            entity.setNightWorkHours(BigDecimal.ZERO);
            entity.setHolidayWorkHours(BigDecimal.ZERO);

            entity.setAllowanceAmount(BigDecimal.ZERO);
            entity.setDeductionAmount(BigDecimal.ZERO);

            entity.setLoanRepaymentAmount(BigDecimal.ZERO);
            entity.setSavingAmount(BigDecimal.ZERO);

            entity.setVehicleUsedFlag(false);
            entity.setMileage(BigDecimal.ZERO);

            entity.setPaidLeaveDays(BigDecimal.ZERO);
            entity.setApprovalStatus(ApprovalStatus.PENDING);

            return;
        }

        entity.setBreakMinutes(
                request.breakMinutes() != null
                        ? request.breakMinutes()
                        : 0
        );

        entity.setWorkHours(
                nvl(request.workHours())
        );

        entity.setOvertimeHours(
                nvl(request.overtimeHours())
        );

        entity.setNightWorkHours(
                nvl(request.nightWorkHours())
        );

        entity.setHolidayWorkHours(
                nvl(request.holidayWorkHours())
        );

        entity.setAllowanceAmount(
                nvl(request.allowanceAmount())
        );

        entity.setDeductionAmount(
                nvl(request.deductionAmount())
        );

        entity.setLoanRepaymentAmount(
                nvl(request.loanRepaymentAmount())
        );

        entity.setSavingAmount(
                nvl(request.savingAmount())
        );

        entity.setVehicleUsedFlag(
                Boolean.TRUE.equals(request.vehicleUsedFlag())
        );

        entity.setMileage(
                nvl(request.mileage())
        );

        entity.setPaidLeaveDays(
                nvl(request.paidLeaveDays())
        );

        entity.setApprovalStatus(
                request.approvalStatus() != null
                        ? request.approvalStatus()
                        : ApprovalStatus.PENDING
        );
    }

    default BigDecimal nvl(BigDecimal value) {
        return value != null
                ? value
                : BigDecimal.ZERO;
    }
}