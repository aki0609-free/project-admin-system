package com.project.backend.features.system.batch.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchScheduleTargetQueryService {

    private final BatchJobDefinitionRepository repository;

    public List<BatchJobDefinition> findScheduleTargets() {
        return repository.findAllByDeletedAtIsNullOrderByIdAsc()
                .stream()
                .filter(BatchJobDefinition::isActiveFlag)
                .filter(BatchJobDefinition::isScheduleEnabled)
                .toList();
    }

    public BatchJobDefinition findScheduleTargetOrNull(Long id) {
        BatchJobDefinition definition = repository.findByIdAndDeletedAtIsNull(id)
                .orElse(null);

        if (definition == null) {
            return null;
        }

        if (!definition.isActiveFlag()) {
            return null;
        }

        if (!definition.isScheduleEnabled()) {
            return null;
        }

        return definition;
    }
}