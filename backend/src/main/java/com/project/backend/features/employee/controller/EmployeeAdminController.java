package com.project.backend.features.employee.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.employee.dto.EmployeeDetailResponse;
import com.project.backend.features.employee.dto.EmployeeListItemResponse;
import com.project.backend.features.employee.dto.EmployeeResignRequest;
import com.project.backend.features.employee.dto.EmployeeResignationChecklistResponse;
import com.project.backend.features.employee.dto.EmployeeSaveRequest;
import com.project.backend.features.employee.service.EmployeeAdminService;
import com.project.backend.features.employee.service.EmployeeResignationChecklistQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeAdminController {

    private final EmployeeAdminService service;
    private final EmployeeResignationChecklistQueryService resignationChecklistQueryService;

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

    @PostMapping
    public EmployeeDetailResponse create(@RequestBody EmployeeSaveRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EmployeeDetailResponse update(
            @PathVariable Long id,
            @RequestBody EmployeeSaveRequest request
    ) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/resign")
    public EmployeeDetailResponse resign(
            @PathVariable Long id,
            @RequestBody EmployeeResignRequest request
    ) {
        return service.resign(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}