package com.project.backend.features.system.backup.service;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.backup.entity.BackupTarget;
import com.project.backend.features.system.backup.repository.BackupTargetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupTargetLookupService {

    private final BackupTargetRepository repository;

    @SuppressWarnings("null")
    public BackupTarget find(Long id) {
        return repository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .orElseThrow(
                        () -> new RuntimeException(
                                "バックアップ定義が見つかりません。 id=" + id
                        )
                );
    }
}