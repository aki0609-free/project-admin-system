package com.project.backend.features.system.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.system.company.dto.CompanyProfileSaveRequest;
import com.project.backend.features.system.company.entity.CompanyProfile;
import com.project.backend.features.system.company.mapper.CompanyProfileMapper;
import com.project.backend.features.system.company.repository.CompanyProfileRepository;

class CompanyProfileCommandServiceTest {

    private CompanyProfileRepository repository;
    private CompanyProfileCommandService service;

    @BeforeEach
    void setUp() {
        repository = mock(CompanyProfileRepository.class);
        service = new CompanyProfileCommandService(
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
    void save_shouldUpdateCurrentCompanyWithoutCreatingAnotherProfile() {
        CompanyProfile current = new CompanyProfile();
        current.setId(10L);
        current.setTenantId("tenant-a");
        current.setCompanyCode("OLD");
        current.setCompanyName("変更前");
        current.setActiveFlag(true);

        when(repository
                .findFirstByTenantIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
                        "tenant-a"
                ))
                .thenReturn(Optional.of(current));
        when(repository
                .existsByTenantIdAndCompanyCodeAndIdNotAndDeletedAtIsNull(
                        "tenant-a",
                        "NEW",
                        10L
                ))
                .thenReturn(false);
        when(repository.save(any(CompanyProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.save(request("NEW", "変更後", false));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.companyCode()).isEqualTo("NEW");
        assertThat(response.companyName()).isEqualTo("変更後");
        assertThat(response.activeFlag()).isTrue();
        assertThat(current.getTenantId()).isEqualTo("tenant-a");
        verify(repository).save(current);
    }

    @Test
    void save_shouldRejectInvalidInvoiceIssuerNumber() {
        CompanyProfileSaveRequest request = request(
                "DEFAULT",
                "株式会社テスト",
                true,
                "12345"
        );

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tと13桁");
    }

    private CompanyProfileSaveRequest request(
            String companyCode,
            String companyName,
            boolean activeFlag
    ) {
        return request(companyCode, companyName, activeFlag, null);
    }

    private CompanyProfileSaveRequest request(
            String companyCode,
            String companyName,
            boolean activeFlag,
            String invoiceIssuerNumber
    ) {
        return new CompanyProfileSaveRequest(
                companyCode,
                companyName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO,
                null,
                invoiceIssuerNumber,
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                activeFlag
        );
    }
}
