package com.project.backend.features.employee.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.employee.dto.EmployeeFinanceTransactionResponse;
import com.project.backend.features.employee.enums.EmployeeFinanceAccountType;
import com.project.backend.features.employee.service.EmployeeFinanceTransactionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employee/finance-transactions")
@RequiredArgsConstructor
public class EmployeeFinanceTransactionController {

    private final EmployeeFinanceTransactionService service;

    @GetMapping
    public List<EmployeeFinanceTransactionResponse> findAll(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) EmployeeFinanceAccountType accountType
    ) {
        return service.findAll(employeeId, accountType);
    }
}
