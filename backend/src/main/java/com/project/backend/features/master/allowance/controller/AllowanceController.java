package com.project.backend.features.master.allowance.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import com.project.backend.features.master.allowance.dto.AllowanceDetailResponse;
import com.project.backend.features.master.allowance.dto.AllowanceListItemResponse;
import com.project.backend.features.master.allowance.dto.AllowanceSaveRequest;
import com.project.backend.features.master.allowance.service.AllowanceCommandService;
import com.project.backend.features.master.allowance.service.AllowanceQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/master/allowances")
@PreAuthorize("hasAuthority('master:view')")
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
    @PreAuthorize("hasAuthority('master:manage')")
    public Long create(
            @Valid @RequestBody AllowanceSaveRequest request
    ) {
        return allowanceCommandService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('master:manage')")
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
    @PreAuthorize("hasAuthority('master:manage')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        allowanceCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
