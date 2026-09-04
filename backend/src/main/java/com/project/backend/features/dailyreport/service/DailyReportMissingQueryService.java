package com.project.backend.features.dailyreport.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportMissingEmployeeResponse;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.service.EmployeeWorkEligibilityPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyReportMissingQueryService {

    private final DailyReportRepository dailyReportRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeContractRepository employeeContractRepository;
    private final EmployeeWorkEligibilityPolicy workEligibilityPolicy;

    @SuppressWarnings("null")
public List<DailyReportMissingEmployeeResponse> findMissingEmployees(
            LocalDate workDate
    ) {
        if (workDate == null) {
            throw new RuntimeException("workDate は必須です。");
        }

        return employeeRepository.findAllByDeletedAtIsNullOrderByIdAsc()
                .stream()
                .filter(employee -> workEligibilityPolicy.isEligible(
                        employee,
                        employeeContractRepository
                                .findByEmployeeIdAndDeletedAtIsNull(employee.getId())
                                .orElse(null),
                        workDate
                ))
                .filter(employee ->
                        !dailyReportRepository.existsByEmployeeIdAndWorkDateAndDeletedAtIsNull(
                                employee.getId(),
                                workDate
                        )
                )
                .sorted(
                        Comparator
                                .comparing(Employee::getEmployeeCode, Comparator.nullsLast(String::compareTo))
                                .thenComparing(Employee::getId)
                )
                .map(employee ->
                        DailyReportMissingEmployeeResponse.builder()
                                .employeeId(employee.getId())
                                .employeeCode(employee.getEmployeeCode())
                                .employeeName(employee.getEmployeeName())
                                .build()
                )
                .toList();
    }
}
