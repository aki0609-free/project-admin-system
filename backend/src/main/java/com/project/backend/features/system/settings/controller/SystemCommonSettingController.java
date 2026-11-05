package com.project.backend.features.system.settings.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.backend.features.system.settings.dto.SystemSettingDefinitionCreateRequest;
import com.project.backend.features.system.settings.dto.SystemSettingDefinitionResponse;
import com.project.backend.features.system.settings.dto.SystemSettingDefinitionUpdateRequest;
import com.project.backend.features.system.settings.dto.SystemSettingItemResponse;
import com.project.backend.features.system.settings.dto.SystemSettingValueSaveRequest;
import com.project.backend.features.system.settings.service.SystemSettingDefinitionService;
import com.project.backend.features.system.settings.service.SystemSettingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/common-settings")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class SystemCommonSettingController {

    private final SystemSettingService systemSettingService;
    private final SystemSettingDefinitionService definitionService;

    @GetMapping
    public List<SystemSettingItemResponse> findAll() {
        return systemSettingService.findAll();
    }

    @PutMapping("/values")
    public List<SystemSettingItemResponse> saveValues(
            @Valid @RequestBody SystemSettingValueSaveRequest request
    ) {
        return systemSettingService.save(request);
    }

    @GetMapping("/definitions")
    public List<SystemSettingDefinitionResponse> findDefinitions() {
        return definitionService.findAll();
    }

    @PostMapping("/definitions")
    public SystemSettingDefinitionResponse createDefinition(
            @Valid @RequestBody SystemSettingDefinitionCreateRequest request
    ) {
        return definitionService.create(request);
    }

    @PutMapping("/definitions/{id}")
    public SystemSettingDefinitionResponse updateDefinition(
            @PathVariable Long id,
            @Valid @RequestBody SystemSettingDefinitionUpdateRequest request
    ) {
        return definitionService.update(id, request);
    }

    @DeleteMapping("/definitions/{id}")
    public void deleteDefinition(@PathVariable Long id) {
        definitionService.delete(id);
    }
}
