package com.project.backend.features.master.deduction.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;

import com.project.backend.features.master.deduction.dto.DeductionDetailResponse;
import com.project.backend.features.master.deduction.dto.DeductionListItemResponse;
import com.project.backend.features.master.deduction.dto.DeductionSaveRequest;
import com.project.backend.features.master.deduction.service.DeductionCommandService;
import com.project.backend.features.master.deduction.service.DeductionQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/master/deductions")
@PreAuthorize("hasAuthority('master:view')")
@RequiredArgsConstructor
public class DeductionController {

    private final DeductionQueryService deductionQueryService;
    private final DeductionCommandService deductionCommandService;

    @GetMapping
    public List<DeductionListItemResponse> findAll() {
        return deductionQueryService.findAll();
    }

    @GetMapping("/{id}")
    public DeductionDetailResponse findDetail(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate targetDate
    ) {
        return deductionQueryService.findDetail(id, targetDate);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('master:manage')")
    public Long create(
            @Valid @RequestBody DeductionSaveRequest request
    ) {
        return deductionCommandService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('master:manage')")
    public void update(
            @PathVariable Long id,
            @Valid @RequestBody DeductionSaveRequest request
    ) {
        deductionCommandService.update(
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('master:manage')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        deductionCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
