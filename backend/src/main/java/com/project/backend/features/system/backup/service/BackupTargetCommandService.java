package com.project.backend.features.system.backup.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.backup.dto.BackupTargetResponse;
import com.project.backend.features.system.backup.dto.BackupTargetSaveRequest;
import com.project.backend.features.system.backup.entity.BackupTarget;
import com.project.backend.features.system.backup.mapper.BackupTargetMapper;
import com.project.backend.features.system.backup.repository.BackupTargetRepository;
import com.project.backend.features.system.backup.service.validation.BackupTargetValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupTargetCommandService {

    private final BackupTargetRepository repository;
    private final BackupTargetLookupService lookupService;
    private final BackupTargetValidator validator;
    private final BackupTargetMapper mapper;

    @Transactional
    public BackupTargetResponse create(BackupTargetSaveRequest request) {
        validator.validate(request);

        BackupTarget entity = new BackupTarget();

        mapper.updateEntityFromRequest(
                request,
                entity
        );

        return mapper.toResponse(
                repository.save(entity)
        );
    }

    @SuppressWarnings("null")
@Transactional
    public BackupTargetResponse update(
            Long id,
            BackupTargetSaveRequest request
    ) {
        validator.validate(request);

        BackupTarget entity = lookupService.find(id);

        mapper.updateEntityFromRequest(
                request,
                entity
        );

        return mapper.toResponse(
                repository.save(entity)
        );
    }

    @Transactional
    public void delete(Long id) {
        BackupTarget entity = lookupService.find(id);

        Instant now = Instant.now();

        entity.setDeletedAt(now);

        if (entity.getColumns() != null) {
            entity.getColumns().forEach(column -> column.setDeletedAt(now));
        }
    }
}