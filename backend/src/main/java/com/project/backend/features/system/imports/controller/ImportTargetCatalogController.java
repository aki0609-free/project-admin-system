package com.project.backend.features.system.imports.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.imports.dto.ImportTargetCatalogResponse;
import com.project.backend.features.system.imports.service.ImportTargetCatalogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/import-target-catalogs")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class ImportTargetCatalogController {

    private final ImportTargetCatalogService service;

    @GetMapping("/active")
    public List<ImportTargetCatalogResponse> findActive() {
        return service.findActive();
    }
}
