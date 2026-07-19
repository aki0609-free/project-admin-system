package com.project.backend.features.employee.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.employee.dto.EmployeeSavingResponse;
import com.project.backend.features.employee.dto.EmployeeSavingSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeSaving;
import com.project.backend.features.employee.enums.ApprovalStatus;

@Component
public class EmployeeSavingMapper {

    public List<EmployeeSavingResponse> toResponseList(List<EmployeeSaving> entities) {
        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    public EmployeeSavingResponse toResponse(EmployeeSaving entity) {
        Employee employee = entity.getEmployee();

        return EmployeeSavingResponse.builder()
                .id(entity.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getEmployeeName())
                .percentage(entity.getPercentage())
                .minSalaryThreshold(entity.getMinSalaryThreshold())
                .currentBalance(entity.getCurrentBalance())
                .activeFlag(entity.isActiveFlag())
                .approvalStatus(entity.getApprovalStatus())
                .approvalComment(entity.getApprovalComment())
                .build();
    }

    public void updateFromRequest(
            EmployeeSavingSaveRequest request,
            EmployeeSaving entity,
            Employee employee
    ) {
        entity.setEmployee(employee);
        entity.setPercentage(nvl(request.getPercentage()));
        entity.setMinSalaryThreshold(nvl(request.getMinSalaryThreshold()));
        entity.setCurrentBalance(nvl(request.getCurrentBalance()));
        entity.setActiveFlag(request.isActiveFlag());
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