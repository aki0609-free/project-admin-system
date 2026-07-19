package com.project.backend.features.system.batch.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.batch.dto.BatchJobDefinitionResponse;
import com.project.backend.features.system.batch.dto.BatchJobDefinitionSaveRequest;
import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.mapper.BatchJobMapper;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;
import com.project.backend.features.system.batch.service.validation.BatchJobDefinitionValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchJobDefinitionCommandService {

    private final BatchJobDefinitionRepository repository;
    private final BatchJobMapper mapper;

    private final BatchJobDefinitionValidator validator;
    private final BatchJobDefinitionLookupService lookupService;
    private final BatchDynamicSchedulerService schedulerService;

    @Transactional
    public BatchJobDefinitionResponse create(
            BatchJobDefinitionSaveRequest request
    ) {
        validator.validate(request, null);

        BatchJobDefinition entity =
                new BatchJobDefinition();

        mapper.updateDefinitionFromRequest(
                request,
                entity
        );

        BatchJobDefinition saved =
                repository.save(entity);

        schedulerService.reloadJob(saved.getId());

        return mapper.toDefinitionResponse(saved);
    }

    @SuppressWarnings("null")
    @Transactional
    public BatchJobDefinitionResponse update(
            Long id,
            BatchJobDefinitionSaveRequest request
    ) {
        validator.validate(request, id);

        BatchJobDefinition entity =
                lookupService.find(id);

        mapper.updateDefinitionFromRequest(
                request,
                entity
        );

        BatchJobDefinition saved =
                repository.save(entity);

        schedulerService.reloadJob(saved.getId());

        return mapper.toDefinitionResponse(saved);
    }

    @Transactional
    public void delete(Long id) {

        BatchJobDefinition entity =
                lookupService.find(id);

        entity.setDeletedAt(
                Instant.now()
        );

        schedulerService.cancel(id);
    }
}