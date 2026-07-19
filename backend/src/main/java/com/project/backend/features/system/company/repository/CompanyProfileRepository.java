package com.project.backend.features.system.company.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.company.entity.CompanyProfile;

public interface CompanyProfileRepository
        extends JpaRepository<CompanyProfile, Long> {

    Optional<CompanyProfile>
            findFirstByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();

    Optional<CompanyProfile>
            findByCompanyCodeAndDeletedAtIsNull(
                    String companyCode
            );

    boolean existsByCompanyCodeAndDeletedAtIsNull(
            String companyCode
    );

    boolean existsByCompanyCodeAndIdNotAndDeletedAtIsNull(
            String companyCode,
            Long id
    );
}