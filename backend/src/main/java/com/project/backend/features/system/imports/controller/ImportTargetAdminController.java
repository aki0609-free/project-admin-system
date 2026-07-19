package com.project.backend.features.system.imports.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.project.backend.features.system.imports.dto.ImportScriptFileResponse;
import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.dto.ImportTargetSaveRequest;
import com.project.backend.features.system.imports.dto.ImportTargetSummary;
import com.project.backend.features.system.imports.service.ImportScriptFileQueryService;
import com.project.backend.features.system.imports.service.ImportTargetAdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/import-targets")
@RequiredArgsConstructor
public class ImportTargetAdminController {

    private final ImportTargetAdminService service;
    private final ImportScriptFileQueryService scriptFileQueryService;


    @GetMapping
    public List<ImportTargetSummary> findAll() {
        return service.findAll();
    }

    @GetMapping("/active")
    public List<ImportTargetSummary> findActiveTargets() {
        return service.findActiveTargets();
    }

    @GetMapping("/{id}")
    public ImportTargetDefinition findDetail(@PathVariable Long id) {
        return service.findDetail(id);
    }

    @PostMapping
    public ImportTargetDefinition create(@RequestBody ImportTargetSaveRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ImportTargetDefinition update(
            @PathVariable Long id,
            @RequestBody ImportTargetSaveRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/scripts")
    public List<ImportScriptFileResponse> findScripts() {
        return scriptFileQueryService.findAll();
    }
}