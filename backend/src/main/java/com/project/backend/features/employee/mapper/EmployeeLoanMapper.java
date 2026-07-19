package com.project.backend.features.employee.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.employee.dto.EmployeeLoanResponse;
import com.project.backend.features.employee.dto.EmployeeLoanSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeLoan;
import com.project.backend.features.employee.enums.ApprovalStatus;

@Component
public class EmployeeLoanMapper {

    public List<EmployeeLoanResponse> toResponseList(List<EmployeeLoan> entities) {
        return entities.stream()
                .map(this::toResponse)
                .toList();
    }

    public EmployeeLoanResponse toResponse(EmployeeLoan entity) {
        Employee employee = entity.getEmployee();

        return EmployeeLoanResponse.builder()
                .id(entity.getId())
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getEmployeeName())
                .principal(entity.getPrincipal())
                .currentBalance(entity.getCurrentBalance())
                .monthlyRepayment(entity.getMonthlyRepayment())
                .loanDate(entity.getLoanDate())
                .repaymentStartDate(entity.getRepaymentStartDate())
                .activeFlag(entity.isActiveFlag())
                .approvalStatus(entity.getApprovalStatus())
                .approvalComment(entity.getApprovalComment())
                .build();
    }

    public void updateFromRequest(
            EmployeeLoanSaveRequest request,
            EmployeeLoan entity,
            Employee employee
    ) {
        entity.setEmployee(employee);
        entity.setPrincipal(nvl(request.getPrincipal()));
        entity.setCurrentBalance(nvl(request.getCurrentBalance()));
        entity.setMonthlyRepayment(nvl(request.getMonthlyRepayment()));
        entity.setLoanDate(request.getLoanDate());
        entity.setRepaymentStartDate(request.getRepaymentStartDate());
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