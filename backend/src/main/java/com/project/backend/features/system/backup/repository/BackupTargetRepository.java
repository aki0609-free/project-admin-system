package com.project.backend.features.system.backup.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.backup.entity.BackupTarget;

public interface BackupTargetRepository extends JpaRepository<BackupTarget, Long> {

    Optional<BackupTarget> findByTargetCodeAndBackupEnabledTrueAndActiveFlagTrueAndDeletedAtIsNull(
            String targetCode
    );

    List<BackupTarget> findByBackupEnabledTrueAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();
}