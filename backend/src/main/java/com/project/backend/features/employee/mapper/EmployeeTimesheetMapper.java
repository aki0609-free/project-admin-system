package com.project.backend.features.employee.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.employee.dto.EmployeeTimesheetResponse;
import com.project.backend.features.employee.dto.EmployeeTimesheetSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeTimesheet;
import com.project.backend.features.employee.enums.ApprovalStatus;

@Component
public class EmployeeTimesheetMapper {

    public List<EmployeeTimesheetResponse> toResponseList(List<EmployeeTimesheet> entities) {
        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    public EmployeeTimesheetResponse toResponse(EmployeeTimesheet entity) {
        Employee employee = entity.getEmployee();

        return EmployeeTimesheetResponse.builder()
                .id(entity.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getEmployeeName())
                .workDate(entity.getWorkDate())
                .clockIn(entity.getClockIn())
                .clockOut(entity.getClockOut())
                .workHours(entity.getWorkHours())
                .overtimeHours(entity.getOvertimeHours())
                .nightShiftHours(entity.getNightShiftHours())
                .weekendFlag(entity.isWeekendFlag())
                .approvalStatus(entity.getApprovalStatus())
                .approvalComment(entity.getApprovalComment())
                .build();
    }

    public void updateFromRequest(
            EmployeeTimesheetSaveRequest request,
            EmployeeTimesheet entity,
            Employee employee
    ) {
        entity.setEmployee(employee);
        entity.setWorkDate(request.getWorkDate());
        entity.setClockIn(request.getClockIn());
        entity.setClockOut(request.getClockOut());
        entity.setWorkHours(nvl(request.getWorkHours()));
        entity.setOvertimeHours(nvl(request.getOvertimeHours()));
        entity.setNightShiftHours(nvl(request.getNightShiftHours()));
        entity.setWeekendFlag(request.isWeekendFlag());
        entity.setApprovalStatus(
                request.getApprovalStatus() != null
                        ? request.getApprovalStatus()
                        : ApprovalStatus.PENDING
        );
        entity.setApprovalComment(request.getApprovalComment());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}