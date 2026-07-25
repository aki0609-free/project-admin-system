package com.project.backend.features.system.backup.service;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.backup.entity.BackupHistory;
import com.project.backend.features.system.backup.repository.BackupHistoryRepository;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupHistoryLookupService {

        private final BackupHistoryRepository repository;

        @SuppressWarnings("null")
        public BackupHistory find(Long id) {
                return repository.findByIdAndTenantIdAndDeletedAtIsNull(
                                                id,
                                                requireTenantId()
                                )
                                .orElseThrow(
                                                () -> new RuntimeException(
                                                                "バックアップ履歴が見つかりません。 id=" + id));
        }

        private String requireTenantId() {
                String tenantId = TenantContext.getTenantId();
                if (tenantId == null || tenantId.isBlank()) {
                        throw new RuntimeException("テナント情報を取得できません。");
                }
                return tenantId;
        }
}
