package com.project.backend.features.system.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.company.entity.CompanyProfile;
import com.project.backend.features.system.company.mapper.CompanyProfileMapper;
import com.project.backend.features.system.company.repository.CompanyProfileRepository;

class CompanyProfileQueryServiceTest {

    private CompanyProfileRepository repository;
    private CompanyProfileQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(CompanyProfileRepository.class);
        service = new CompanyProfileQueryService(
                repository,
                new CompanyProfileMapper()
        );
        TenantContext.setTenantId("tenant-a");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findCurrentOrNull_shouldQueryCurrentTenantExplicitly() {
        CompanyProfile entity = new CompanyProfile();
        entity.setId(1L);
        entity.setCompanyCode("DEFAULT");
        entity.setCompanyName("株式会社テスト");
        entity.setActiveFlag(true);

        when(repository
                .findFirstByTenantIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
                        "tenant-a"
                ))
                .thenReturn(Optional.of(entity));

        assertThat(service.findCurrentOrNull().companyName())
                .isEqualTo("株式会社テスト");
        verify(repository)
                .findFirstByTenantIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
                        "tenant-a"
                );
    }
}
