package com.project.backend.features.system.batch.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.system.batch.dto.BatchJobDefinitionResponse;
import com.project.backend.features.system.batch.mapper.BatchJobMapper;
import com.project.backend.features.system.batch.repository.BatchJobDefinitionRepository;
import com.project.backend.app.tenant.context.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchJobDefinitionQueryService {

    private final BatchJobDefinitionRepository repository;
    private final BatchJobMapper mapper;
    private final BatchJobDefinitionLookupService lookupService;

    public List<BatchJobDefinitionResponse> findAll() {
        return mapper.toDefinitionResponseList(
                repository.findAllByTenantIdAndDeletedAtIsNullOrderByIdAsc(
                        requireTenantId()
                )
        );
    }

    public BatchJobDefinitionResponse findDetail(Long id) {
        return mapper.toDefinitionResponse(
                lookupService.find(id)
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
