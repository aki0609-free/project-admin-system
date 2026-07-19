package com.project.backend.features.employee.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.employee.dto.EmployeeSavingResponse;
import com.project.backend.features.employee.dto.EmployeeSavingSaveRequest;
import com.project.backend.features.employee.service.EmployeeSavingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employee/savings")
@RequiredArgsConstructor
public class EmployeeSavingController {

    private final EmployeeSavingService service;

    @GetMapping
    public List<EmployeeSavingResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public EmployeeSavingResponse findDetail(@PathVariable Long id) {
        return service.findDetail(id);
    }

    @PostMapping
    public EmployeeSavingResponse create(
            @RequestBody EmployeeSavingSaveRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EmployeeSavingResponse update(
            @PathVariable Long id,
            @RequestBody EmployeeSavingSaveRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}