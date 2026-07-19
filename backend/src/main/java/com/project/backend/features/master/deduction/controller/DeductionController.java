package com.project.backend.features.master.deduction.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.master.deduction.dto.DeductionDetailResponse;
import com.project.backend.features.master.deduction.dto.DeductionListItemResponse;
import com.project.backend.features.master.deduction.dto.DeductionSaveRequest;
import com.project.backend.features.master.deduction.service.DeductionCommandService;
import com.project.backend.features.master.deduction.service.DeductionQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/master/deductions")
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
            @PathVariable Long id
    ) {
        return deductionQueryService.findDetail(id);
    }

    @PostMapping
    public Long create(
            @Valid @RequestBody DeductionSaveRequest request
    ) {
        return deductionCommandService.create(request);
    }

    @PutMapping("/{id}")
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
    public void delete(
            @PathVariable Long id
    ) {
        deductionCommandService.delete(id);
    }
}