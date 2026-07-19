package com.project.backend.features.master.allowance.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.master.allowance.dto.AllowanceDetailResponse;
import com.project.backend.features.master.allowance.dto.AllowanceListItemResponse;
import com.project.backend.features.master.allowance.dto.AllowanceSaveRequest;
import com.project.backend.features.master.allowance.service.AllowanceCommandService;
import com.project.backend.features.master.allowance.service.AllowanceQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/master/allowances")
@RequiredArgsConstructor
public class AllowanceController {

    private final AllowanceQueryService allowanceQueryService;
    private final AllowanceCommandService allowanceCommandService;

    @GetMapping
    public List<AllowanceListItemResponse> findAll() {
        return allowanceQueryService.findAll();
    }

    @GetMapping("/{id}")
    public AllowanceDetailResponse findDetail(
            @PathVariable Long id
    ) {
        return allowanceQueryService.findDetail(id);
    }

    @PostMapping
    public Long create(
            @Valid @RequestBody AllowanceSaveRequest request
    ) {
        return allowanceCommandService.create(request);
    }

    @PutMapping("/{id}")
    public void update(
            @PathVariable Long id,
            @Valid @RequestBody AllowanceSaveRequest request
    ) {
        allowanceCommandService.update(
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id
    ) {
        allowanceCommandService.delete(id);
    }
}