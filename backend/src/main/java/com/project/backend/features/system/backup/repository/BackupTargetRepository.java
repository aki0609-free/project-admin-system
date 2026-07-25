package com.project.backend.features.system.backup.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.backup.entity.BackupTarget;

public interface BackupTargetRepository extends JpaRepository<BackupTarget, Long> {

    Optional<BackupTarget> findByTenantIdAndTargetCodeAndBackupEnabledTrueAndActiveFlagTrueAndDeletedAtIsNull(
            String tenantId,
            String targetCode
    );

    List<BackupTarget> findByTenantIdAndBackupEnabledTrueAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
            String tenantId
    );

    Optional<BackupTarget> findByIdAndTenantIdAndDeletedAtIsNull(
            Long id,
            String tenantId
    );

    boolean existsByTenantIdAndTargetCodeAndDeletedAtIsNull(
            String tenantId,
            String targetCode
    );

    boolean existsByTenantIdAndTargetCodeAndIdNotAndDeletedAtIsNull(
            String tenantId,
            String targetCode,
            Long id
    );
}
