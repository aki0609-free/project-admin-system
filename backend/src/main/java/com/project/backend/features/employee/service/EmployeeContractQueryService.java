package com.project.backend.features.employee.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.dto.EmployeeContractQueryResponse;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.employee.repository.EmployeeContractRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeContractQueryService {

    private final EmployeeContractRepository repository;

    public EmployeeContractQueryResponse findByEmployeeId(
            Long employeeId) {
        if (employeeId == null) {
            throw new RuntimeException("employeeId は必須です。");
        }

        EmployeeContract entity = repository.findByEmployeeIdAndDeletedAtIsNull(employeeId)
                .orElse(null);

        if (entity == null) {
            return emptyResponse(employeeId);
        }

        return toResponse(entity);
    }

    private EmployeeContractQueryResponse toResponse(
            EmployeeContract entity) {
        Employee employee = entity.getEmployee();

        return EmployeeContractQueryResponse.builder()
                .id(entity.getId())

                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getEmployeeName())

                .contractStartDate(entity.getContractStartDate())
                .contractEndDate(entity.getContractEndDate())
                .renewalFlag(entity.isRenewalFlag())

                .salaryType(entity.getSalaryType())
                .paymentCycle(entity.getPaymentCycle())

                .monthlySalary(entity.getMonthlySalary())
                .weeklyWage(entity.getWeeklyWage())
                .dailyWage(entity.getDailyWage())
                .hourlyWage(entity.getHourlyWage())

                .standardWorkingHours(entity.getStandardWorkingHours())

                .note(entity.getNote())
                .build();
    }

    private EmployeeContractQueryResponse emptyResponse(
            Long employeeId) {
        return EmployeeContractQueryResponse.builder()
                .id(null)

                .employeeId(employeeId)
                .employeeCode(null)
                .employeeName(null)

                .contractStartDate(null)
                .contractEndDate(null)
                .renewalFlag(false)

                .salaryType(SalaryType.MONTHLY)
                .paymentCycle(PaymentCycle.MONTHLY)

                .monthlySalary(java.math.BigDecimal.ZERO)
                .weeklyWage(java.math.BigDecimal.ZERO)
                .dailyWage(java.math.BigDecimal.ZERO)
                .hourlyWage(java.math.BigDecimal.ZERO)

                .standardWorkingHours(java.math.BigDecimal.ZERO)

                .note(null)
                .build();
    }
}