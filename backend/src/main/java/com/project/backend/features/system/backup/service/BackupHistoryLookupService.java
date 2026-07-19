package com.project.backend.features.system.backup.service;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.backup.entity.BackupHistory;
import com.project.backend.features.system.backup.repository.BackupHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupHistoryLookupService {

        private final BackupHistoryRepository repository;

        @SuppressWarnings("null")
        public BackupHistory find(Long id) {
                return repository.findById(id)
                                .filter(entity -> entity.getDeletedAt() == null)
                                .orElseThrow(
                                                () -> new RuntimeException(
                                                                "バックアップ履歴が見つかりません。 id=" + id));
        }
}