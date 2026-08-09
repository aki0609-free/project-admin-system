package com.project.backend.features.employee.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.employee.dto.EmployeeDetailResponse;
import com.project.backend.features.employee.dto.EmployeeListItemResponse;
import com.project.backend.features.employee.dto.EmployeeResignRequest;
import com.project.backend.features.employee.dto.EmployeeResignationChecklistResponse;
import com.project.backend.features.employee.dto.EmployeeResignationConfigurationResponse;
import com.project.backend.features.employee.dto.EmployeeSaveRequest;
import com.project.backend.features.employee.service.EmployeeAdminService;
import com.project.backend.features.employee.service.EmployeeResignationChecklistQueryService;
import com.project.backend.features.master.payrollitem.balance.EmployeePayrollItemSettingService;
import com.project.backend.features.employee.dto.EmployeePayrollItemSettingResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeAdminController {

    private final EmployeeAdminService service;
    private final EmployeeResignationChecklistQueryService resignationChecklistQueryService;
    private final EmployeePayrollItemSettingService payrollItemSettingService;

    @GetMapping("/payroll-item-settings/catalog")
    public List<EmployeePayrollItemSettingResponse> findPayrollItemSettingCatalog() {
        return payrollItemSettingService.findCatalog();
    }

    @GetMapping
    public List<EmployeeListItemResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public EmployeeDetailResponse findDetail(@PathVariable Long id) {
        return service.findDetail(id);
    }

    @GetMapping("/resignation-checklist")
    public List<EmployeeResignationChecklistResponse> findResignationChecklist() {
        return resignationChecklistQueryService.findAllActive();
    }

    @GetMapping("/resignation-configuration")
    public EmployeeResignationConfigurationResponse findResignationConfiguration() {
        return resignationChecklistQueryService.findConfiguration();
    }

    @PostMapping
    public EmployeeDetailResponse create(@Valid @RequestBody EmployeeSaveRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EmployeeDetailResponse update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeSaveRequest request
    ) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/resign")
    public EmployeeDetailResponse resign(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeResignRequest request
    ) {
        return service.resign(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/cancel-resignation")
    public EmployeeDetailResponse cancelResignation(@PathVariable Long id) {
        return service.cancelResignation(id);
    }
}
