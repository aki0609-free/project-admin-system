package com.project.backend.features.system.backup.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.backup.dto.BackupTargetDefinition;
import com.project.backend.features.system.backup.dto.BackupTargetSummary;
import com.project.backend.features.system.backup.mapper.BackupTargetMapper;
import com.project.backend.features.system.backup.repository.BackupTargetRepository;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupDefinitionService {

    private final BackupTargetRepository repository;
    private final BackupTargetMapper mapper;

    @Transactional(readOnly = true)
    public BackupTargetDefinition getBackupTargetDefinition(String targetCode) {
        return repository
                .findByTenantIdAndTargetCodeAndBackupEnabledTrueAndActiveFlagTrueAndDeletedAtIsNull(
                        requireTenantId(),
                        targetCode
                )
                .map(mapper::toDefinition)
                .orElseThrow(
                        () -> new RuntimeException(
                                "有効なバックアップ対象定義が見つかりません。 targetCode=" + targetCode
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<BackupTargetSummary> findBackupEnabledTargets() {
        return repository
                .findByTenantIdAndBackupEnabledTrueAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
                        requireTenantId()
                )
                .stream()
                .map(mapper::toSummary)
                .toList();
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new RuntimeException("テナント情報を取得できません。");
        }
        return tenantId;
    }
}
