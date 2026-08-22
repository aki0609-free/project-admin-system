package com.project.backend.features.system.company.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.company.dto.CompanyProfileResponse;
import com.project.backend.features.system.company.entity.CompanyProfile;
import com.project.backend.features.system.company.mapper.CompanyProfileMapper;
import com.project.backend.features.system.company.repository.CompanyProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyProfileQueryService {

    private final CompanyProfileRepository repository;
    private final CompanyProfileMapper mapper;

    public CompanyProfileResponse findCurrent() {
        return mapper.toResponse(
                findCurrentEntity()
        );
    }

    public CompanyProfileResponse findCurrentOrNull() {
        return repository
                .findFirstByTenantIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
                        requireTenantId()
                )
                .map(mapper::toResponse)
                .orElse(null);
    }

    public CompanyProfileResponse findByCompanyCode(
            String companyCode
    ) {
        CompanyProfile entity = repository
                .findByTenantIdAndCompanyCodeAndDeletedAtIsNull(
                        requireTenantId(),
                        companyCode
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "会社情報が見つかりません。"
                                        + " companyCode="
                                        + companyCode
                        )
                );

        return mapper.toResponse(entity);
    }

    public CompanyProfile findCurrentEntity() {
        return repository
                .findFirstByTenantIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
                        requireTenantId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "有効な会社情報が登録されていません。"
                        )
                );
    }

    private String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("テナント情報が取得できません。");
        }
        return tenantId;
    }
}
