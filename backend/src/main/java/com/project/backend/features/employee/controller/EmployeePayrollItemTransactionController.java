package com.project.backend.features.employee.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.master.payrollitem.transaction.EmployeePayrollItemTransactionRequest;
import com.project.backend.features.master.payrollitem.transaction.EmployeePayrollItemTransactionResponse;
import com.project.backend.features.master.payrollitem.transaction.EmployeePayrollItemTransactionService;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employees/{employeeId}/payroll-item-transactions")
@RequiredArgsConstructor
public class EmployeePayrollItemTransactionController {

    private final EmployeePayrollItemTransactionService service;

    @GetMapping
    public List<EmployeePayrollItemTransactionResponse> findAll(
            @PathVariable Long employeeId,
            @RequestParam PayrollItemTargetType targetType,
            @RequestParam String targetCode,
            @RequestParam String targetMonth
    ) {
        return service.findAll(employeeId, targetType, targetCode, targetMonth);
    }

    @PostMapping
    public EmployeePayrollItemTransactionResponse create(
            @PathVariable Long employeeId,
            @Valid @RequestBody EmployeePayrollItemTransactionRequest request
    ) {
        return service.create(employeeId, request);
    }

    @PutMapping("/{transactionId}")
    public EmployeePayrollItemTransactionResponse update(
            @PathVariable Long employeeId,
            @PathVariable Long transactionId,
            @Valid @RequestBody EmployeePayrollItemTransactionRequest request
    ) {
        return service.update(employeeId, transactionId, request);
    }

    @DeleteMapping("/{transactionId}")
    public void delete(
            @PathVariable Long employeeId,
            @PathVariable Long transactionId
    ) {
        service.delete(employeeId, transactionId);
    }
}
