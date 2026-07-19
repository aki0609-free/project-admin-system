package com.project.backend.features.system.imports.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.imports.dto.ImportTargetDefinition;
import com.project.backend.features.system.imports.dto.ImportTargetSaveRequest;
import com.project.backend.features.system.imports.entity.ImportTarget;
import com.project.backend.features.system.imports.mapper.ImportTargetMapper;
import com.project.backend.features.system.imports.repository.ImportTargetRepository;
import com.project.backend.features.system.imports.service.validation.ImportTargetValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImportTargetCommandService {

    private final ImportTargetRepository repository;
    private final ImportTargetLookupService lookupService;
    private final ImportTargetApplyService applyService;
    private final ImportTargetValidator validator;
    private final ImportTargetMapper mapper;

    @Transactional
    public ImportTargetDefinition create(ImportTargetSaveRequest request) {
        validator.validateForCreate(request);

        ImportTarget entity = new ImportTarget();
        applyService.apply(entity, request);

        return mapper.toDefinition(repository.save(entity));
    }

    @SuppressWarnings("null")
    @Transactional
    public ImportTargetDefinition update(Long id, ImportTargetSaveRequest request) {
        validator.validateForUpdate(id, request);

        ImportTarget entity = lookupService.find(id);
        applyService.apply(entity, request);

        return mapper.toDefinition(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        ImportTarget entity = lookupService.find(id);

        Instant now = Instant.now();
        entity.setDeletedAt(now);

        if (entity.getColumns() != null) {
            entity.getColumns().forEach(column -> column.setDeletedAt(now));
        }
    }
}