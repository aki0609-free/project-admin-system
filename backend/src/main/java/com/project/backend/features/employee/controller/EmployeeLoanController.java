package com.project.backend.features.employee.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.employee.dto.EmployeeLoanResponse;
import com.project.backend.features.employee.dto.EmployeeLoanSaveRequest;
import com.project.backend.features.employee.service.EmployeeLoanService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employee/loans")
@RequiredArgsConstructor
public class EmployeeLoanController {

    private final EmployeeLoanService service;

    @GetMapping
    public List<EmployeeLoanResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public EmployeeLoanResponse findDetail(@PathVariable Long id) {
        return service.findDetail(id);
    }

    @PostMapping
    public EmployeeLoanResponse create(
            @RequestBody EmployeeLoanSaveRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EmployeeLoanResponse update(
            @PathVariable Long id,
            @RequestBody EmployeeLoanSaveRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}