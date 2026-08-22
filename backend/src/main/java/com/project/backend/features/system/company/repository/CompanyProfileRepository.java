package com.project.backend.features.system.company.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.company.entity.CompanyProfile;

public interface CompanyProfileRepository
        extends JpaRepository<CompanyProfile, Long> {

    Optional<CompanyProfile>
            findFirstByTenantIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
                    String tenantId
            );

    Optional<CompanyProfile>
            findByTenantIdAndCompanyCodeAndDeletedAtIsNull(
                    String tenantId,
                    String companyCode
            );

    boolean existsByTenantIdAndCompanyCodeAndDeletedAtIsNull(
            String tenantId,
            String companyCode
    );

    boolean existsByTenantIdAndCompanyCodeAndIdNotAndDeletedAtIsNull(
            String tenantId,
            String companyCode,
            Long id
    );
}
