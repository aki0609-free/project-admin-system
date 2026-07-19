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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyReportMissingQueryService {

    private final DailyReportRepository dailyReportRepository;
    private final EmployeeRepository employeeRepository;

    @SuppressWarnings("null")
public List<DailyReportMissingEmployeeResponse> findMissingEmployees(
            LocalDate workDate
    ) {
        if (workDate == null) {
            throw new RuntimeException("workDate は必須です。");
        }

        return employeeRepository.findAllByDeletedAtIsNullOrderByIdAsc()
                .stream()
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