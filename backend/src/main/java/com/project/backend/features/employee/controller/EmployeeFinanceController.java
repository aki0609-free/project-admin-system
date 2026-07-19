package com.project.backend.features.employee.controller;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.employee.dto.EmployeeFinanceSummaryResponse;
import com.project.backend.features.employee.service.EmployeeFinanceQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employees")
public class EmployeeFinanceController {

    private final EmployeeFinanceQueryService service;

    @GetMapping("/{employeeId}/finance-summary")
    public EmployeeFinanceSummaryResponse findSummary(
            @PathVariable Long employeeId
    ) {
        return service.findSummary(employeeId);
    }
}