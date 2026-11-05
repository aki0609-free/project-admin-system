package com.project.backend.features.system.masterdata.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.masterdata.entity.GenericMasterRecord;

public interface GenericMasterRecordRepository
        extends JpaRepository<GenericMasterRecord, Long> {

    List<GenericMasterRecord>
            findByTenantIdAndDefinitionIdAndDeletedAtIsNullOrderByIdAsc(
                    String tenantId,
                    Long definitionId
            );

    List<GenericMasterRecord>
            findByTenantIdAndDefinitionIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc(
                    String tenantId,
                    Long definitionId
            );

    Optional<GenericMasterRecord> findByIdAndTenantIdAndDefinitionIdAndDeletedAtIsNull(
            Long id,
            String tenantId,
            Long definitionId
    );

    boolean existsByTenantIdAndDefinitionIdAndDeletedAtIsNull(
            String tenantId,
            Long definitionId
    );
}
