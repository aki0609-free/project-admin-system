package com.project.backend.features.system.batch.service;

import org.springframework.stereotype.Service;

import com.project.backend.features.system.batch.entity.BatchJobDefinition;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchJobDefinitionLookupService {

    private final BatchJobDefinitionRepository repository;

    public BatchJobDefinition find(Long id) {
        return repository.findByIdAndTenantIdAndDeletedAtIsNull(
                        id,
                        requireTenantId()
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "バッチ定義が見つかりません。 id=" + id
                        )
                );
    }

    public BatchJobDefinition findActiveByJobCode(String jobCode) {
        return repository
                .findByTenantIdAndJobCodeAndActiveFlagTrueAndDeletedAtIsNull(
                        requireTenantId(),
                        jobCode
                )
                .orElseThrow(
                        () -> new RuntimeException(
                                "有効なバッチ定義が見つかりません。 jobCode=" + jobCode
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
