package com.project.backend.features.tax.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.tax.dto.StandardRemunerationConfirmRequest;
import com.project.backend.features.tax.dto.StandardRemunerationDraftSaveRequest;
import com.project.backend.features.tax.dto.StandardRemunerationEditorResponse;
import com.project.backend.features.tax.service.StandardRemunerationEditorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/master/deductions/standard-remunerations")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class StandardRemunerationEditorController {
    private final StandardRemunerationEditorService service;

    @GetMapping
    public StandardRemunerationEditorResponse find() {
        return service.findEditor();
    }

    @PutMapping("/draft")
    public StandardRemunerationEditorResponse saveDraft(
            @RequestBody StandardRemunerationDraftSaveRequest request) {
        return service.saveDraft(request);
    }

    @PostMapping("/{batchId}/confirm")
    public StandardRemunerationEditorResponse confirm(
            @PathVariable Long batchId,
            @RequestBody StandardRemunerationConfirmRequest request) {
        return service.confirm(batchId, request);
    }
}
