package com.project.backend.features.employee.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.employee.dto.EmployeeTimesheetResponse;
import com.project.backend.features.employee.dto.EmployeeTimesheetSaveRequest;
import com.project.backend.features.employee.service.EmployeeTimesheetService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/employee/timesheets")
@RequiredArgsConstructor
public class EmployeeTimesheetController {

    private final EmployeeTimesheetService service;

    @GetMapping
    public List<EmployeeTimesheetResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public EmployeeTimesheetResponse findDetail(@PathVariable Long id) {
        return service.findDetail(id);
    }

    @PostMapping
    public EmployeeTimesheetResponse create(
            @RequestBody EmployeeTimesheetSaveRequest request
    ) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EmployeeTimesheetResponse update(
            @PathVariable Long id,
            @RequestBody EmployeeTimesheetSaveRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}