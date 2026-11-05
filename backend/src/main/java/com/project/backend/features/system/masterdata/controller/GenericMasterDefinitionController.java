package com.project.backend.features.system.masterdata.controller;

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

import com.project.backend.features.system.masterdata.dto.GenericMasterDefinitionResponse;
import com.project.backend.features.system.masterdata.dto.GenericMasterDefinitionSaveRequest;
import com.project.backend.features.system.masterdata.service.GenericMasterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/generic-masters/definitions")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class GenericMasterDefinitionController {

    private final GenericMasterService service;

    @GetMapping
    public List<GenericMasterDefinitionResponse> findAll() {
        return service.findDefinitions();
    }

    @PostMapping
    public GenericMasterDefinitionResponse create(
            @Valid @RequestBody GenericMasterDefinitionSaveRequest request
    ) {
        return service.createDefinition(request);
    }

    @PutMapping("/{id}")
    public GenericMasterDefinitionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody GenericMasterDefinitionSaveRequest request
    ) {
        return service.updateDefinition(id, request);
    }

    @PostMapping("/{id}/publish")
    public GenericMasterDefinitionResponse publish(@PathVariable Long id) {
        return service.publish(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteDefinition(id);
    }
}
