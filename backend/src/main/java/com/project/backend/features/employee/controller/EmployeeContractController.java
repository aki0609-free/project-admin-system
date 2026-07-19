package com.project.backend.features.employee.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.employee.dto.EmployeeContractQueryResponse;
import com.project.backend.features.employee.service.EmployeeContractQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class EmployeeContractController {

    private final EmployeeContractQueryService queryService;

    @GetMapping("/api/employees/{employeeId}/contract")
    public EmployeeContractQueryResponse findByEmployeeId(
            @PathVariable Long employeeId
    ) {
        return queryService.findByEmployeeId(employeeId);
    }
}