package com.project.backend.features.system.backup.service;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.backup.entity.BackupTarget;
import com.project.backend.features.system.backup.repository.BackupTargetRepository;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupTargetLookupService {

    private final BackupTargetRepository repository;

    @SuppressWarnings("null")
    public BackupTarget find(Long id) {
        return repository.findByIdAndTenantIdAndDeletedAtIsNull(
                        id,
                        requireTenantId()
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "バックアップ定義が見つかりません。 id=" + id
                        )
                );
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new RuntimeException("テナント情報を取得できません。");
        }
        return tenantId;
    }
}
