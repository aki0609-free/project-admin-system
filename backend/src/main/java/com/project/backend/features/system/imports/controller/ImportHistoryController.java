package com.project.backend.features.system.imports.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.project.backend.features.system.imports.dto.ImportErrorRowResponse;
import com.project.backend.features.system.imports.dto.ImportHistoryResponse;
import com.project.backend.features.system.imports.service.ImportHistoryQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/import-history")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class ImportHistoryController {

    private final ImportHistoryQueryService service;

    @GetMapping
    public List<ImportHistoryResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{historyId}/errors")
    public List<ImportErrorRowResponse> findErrors(
            @PathVariable Long historyId
    ) {
        return service.findErrors(historyId);
    }
}
