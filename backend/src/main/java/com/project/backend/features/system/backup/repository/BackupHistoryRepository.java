package com.project.backend.features.system.backup.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.backup.entity.BackupHistory;

public interface BackupHistoryRepository extends JpaRepository<BackupHistory, Long> {

    List<BackupHistory> findTop200ByTenantIdAndDeletedAtIsNullOrderByExecutedAtDesc(
            String tenantId
    );

    Optional<BackupHistory> findByIdAndTenantIdAndDeletedAtIsNull(
            Long id,
            String tenantId
    );
}
