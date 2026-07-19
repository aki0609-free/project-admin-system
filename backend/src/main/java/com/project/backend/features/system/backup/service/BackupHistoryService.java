package com.project.backend.features.system.backup.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.backup.dto.BackupExecutionResult;
import com.project.backend.features.system.backup.dto.BackupHistoryResponse;
import com.project.backend.features.system.backup.dto.BackupRequest;
import com.project.backend.features.system.backup.repository.BackupHistoryRepository;
import com.project.backend.features.system.backup.service.builder.BackupHistoryBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupHistoryService {

    private final BackupHistoryRepository repository;
    private final BackupHistoryBuilder builder;

    @Transactional(readOnly = true)
    public List<BackupHistoryResponse> findAll() {
        return repository.findByDeletedAtIsNullOrderByExecutedAtDesc()
                .stream()
                .map(BackupHistoryResponse::from)
                .toList();
    }

    @SuppressWarnings("null")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSuccess(
            BackupRequest request,
            BackupExecutionResult result
    ) {
        repository.save(
                builder.buildSuccess(
                        request,
                        result
                )
        );
    }

    @SuppressWarnings("null")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailure(
            BackupRequest request,
            Exception exception
    ) {
        repository.save(
                builder.buildFailure(
                        request,
                        exception
                )
        );
    }
}