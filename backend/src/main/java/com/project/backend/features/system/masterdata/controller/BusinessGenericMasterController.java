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
import com.project.backend.features.system.masterdata.dto.GenericMasterRecordResponse;
import com.project.backend.features.system.masterdata.dto.GenericMasterRecordSaveRequest;
import com.project.backend.features.system.masterdata.entity.GenericMasterPlacement;
import com.project.backend.features.system.masterdata.service.GenericMasterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/business-settings/generic-masters")
@PreAuthorize("hasRole('SYS_ADMIN')")
@RequiredArgsConstructor
public class BusinessGenericMasterController {

    private final GenericMasterService service;

    @GetMapping
    public List<GenericMasterDefinitionResponse> findPublishedDefinitions() {
        return service.findPublishedDefinitions(GenericMasterPlacement.BUSINESS_SETTINGS);
    }

    @GetMapping("/{definitionId}/records")
    public List<GenericMasterRecordResponse> findRecords(
            @PathVariable Long definitionId
    ) {
        return service.findRecords(definitionId);
    }

    @PostMapping("/{definitionId}/records")
    public GenericMasterRecordResponse createRecord(
            @PathVariable Long definitionId,
            @Valid @RequestBody GenericMasterRecordSaveRequest request
    ) {
        return service.createRecord(definitionId, request);
    }

    @PutMapping("/{definitionId}/records/{recordId}")
    public GenericMasterRecordResponse updateRecord(
            @PathVariable Long definitionId,
            @PathVariable Long recordId,
            @Valid @RequestBody GenericMasterRecordSaveRequest request
    ) {
        return service.updateRecord(definitionId, recordId, request);
    }

    @DeleteMapping("/{definitionId}/records/{recordId}")
    public void deleteRecord(
            @PathVariable Long definitionId,
            @PathVariable Long recordId
    ) {
        service.deleteRecord(definitionId, recordId);
    }
}
