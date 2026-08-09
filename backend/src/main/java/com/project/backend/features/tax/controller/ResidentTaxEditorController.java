package com.project.backend.features.tax.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.project.backend.features.tax.dto.*;
import com.project.backend.features.tax.service.ResidentTaxEditorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/master/deductions/resident-tax")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class ResidentTaxEditorController {
    private final ResidentTaxEditorService service;

    @GetMapping
    public ResidentTaxEditorResponse find(@RequestParam Integer fiscalYear) {
        return service.findEditor(fiscalYear);
    }

    @PutMapping("/draft")
    public ResidentTaxEditorResponse saveDraft(@RequestBody ResidentTaxDraftSaveRequest request) {
        return service.saveDraft(request);
    }

    @PostMapping("/{batchId}/confirm")
    public ResidentTaxEditorResponse confirm(
            @PathVariable Long batchId,
            @RequestBody ResidentTaxConfirmRequest request) {
        return service.confirm(batchId, request);
    }
}
