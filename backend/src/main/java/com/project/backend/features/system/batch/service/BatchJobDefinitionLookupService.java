package com.project.backend.features.system.batch.service;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchJobDefinitionLookupService {

    private final BatchJobDefinitionRepository repository;

    public BatchJobDefinition find(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "バッチ定義が見つかりません。 id=" + id
                        )
                );
    }

    public BatchJobDefinition findActiveByJobCode(String jobCode) {
        return repository
                .findByJobCodeAndActiveFlagTrueAndDeletedAtIsNull(jobCode)
                .orElseThrow(
                        () -> new RuntimeException(
                                "有効なバッチ定義が見つかりません。 jobCode=" + jobCode
                        )
                );
    }
}