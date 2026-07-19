package com.project.backend.features.operation.preparation.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.operation.preparation.dto.DailyPreparationAssignmentBulkSaveRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationAssignmentResponse;
import com.project.backend.features.operation.preparation.dto.DailyPreparationAssignmentSaveRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationCreateRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationDispatchBulkSaveRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationDispatchResponse;
import com.project.backend.features.operation.preparation.dto.DailyPreparationDispatchSaveRequest;
import com.project.backend.features.operation.preparation.dto.DailyPreparationResponse;
import com.project.backend.features.operation.preparation.service.DailyPreparationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/operation/daily-preparations")
@RequiredArgsConstructor
public class DailyPreparationController {

    private final DailyPreparationService service;

    @GetMapping
    public DailyPreparationResponse findByTargetDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate) {
        return service.findByTargetDate(targetDate);
    }

    @PostMapping
    public DailyPreparationResponse create(
            @Valid @RequestBody DailyPreparationCreateRequest request) {
        return service.create(request);
    }

    @PostMapping("/assignments")
    public DailyPreparationAssignmentResponse createAssignment(
            @Valid @RequestBody DailyPreparationAssignmentSaveRequest request) {
        return service.createAssignment(request);
    }

    @PutMapping("/assignments/{id}")
    public DailyPreparationAssignmentResponse updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody DailyPreparationAssignmentSaveRequest request) {
        return service.updateAssignment(id, request);
    }

    @DeleteMapping("/assignments/{id}")
    public void deleteAssignment(
            @PathVariable Long id) {
        service.deleteAssignment(id);
    }

    @PostMapping("/dispatches")
    public DailyPreparationDispatchResponse createDispatch(
            @Valid @RequestBody DailyPreparationDispatchSaveRequest request) {
        return service.createDispatch(request);
    }

    @PutMapping("/dispatches/{id}")
    public DailyPreparationDispatchResponse updateDispatch(
            @PathVariable Long id,
            @Valid @RequestBody DailyPreparationDispatchSaveRequest request) {
        return service.updateDispatch(id, request);
    }

    @DeleteMapping("/dispatches/{id}")
    public void deleteDispatch(
            @PathVariable Long id) {
        service.deleteDispatch(id);
    }

    @PostMapping("/assignments/bulk-save")
    public DailyPreparationResponse bulkSaveAssignments(
            @Valid @RequestBody DailyPreparationAssignmentBulkSaveRequest request) {
        return service.bulkSaveAssignments(request);
    }

    @PostMapping("/dispatches/bulk-save")
    public DailyPreparationResponse bulkSaveDispatches(
            @Valid @RequestBody DailyPreparationDispatchBulkSaveRequest request) {
        return service.bulkSaveDispatches(request);
    }
}