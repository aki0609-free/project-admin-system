package com.project.backend.features.system.imports.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.dto.ImportTargetSaveRequest;
import com.project.backend.features.system.imports.dto.ImportTargetSummary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportTargetAdminService {

    private final ImportTargetQueryService queryService;
    private final ImportTargetCommandService commandService;

    public List<ImportTargetSummary> findAll() {
        return queryService.findAll();
    }

    public List<ImportTargetSummary> findActiveTargets() {
        return queryService.findActiveTargets();
    }

    public ImportTargetDefinition findDetail(Long id) {
        return queryService.findDetail(id);
    }

    public ImportTargetDefinition findByTargetCode(String targetCode) {
        return queryService.findByTargetCode(targetCode);
    }

    public ImportTargetDefinition create(ImportTargetSaveRequest request) {
        return commandService.create(request);
    }

    public ImportTargetDefinition update(Long id, ImportTargetSaveRequest request) {
        return commandService.update(id, request);
    }

    public void delete(Long id) {
        commandService.delete(id);
    }
}