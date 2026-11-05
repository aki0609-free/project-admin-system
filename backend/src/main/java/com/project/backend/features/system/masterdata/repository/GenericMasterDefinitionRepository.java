package com.project.backend.features.system.masterdata.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.masterdata.entity.GenericMasterDefinition;
import com.project.backend.features.system.masterdata.entity.GenericMasterPlacement;
import com.project.backend.features.system.masterdata.entity.GenericMasterStatus;

public interface GenericMasterDefinitionRepository
        extends JpaRepository<GenericMasterDefinition, Long> {

    List<GenericMasterDefinition> findByTenantIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
            String tenantId
    );

    List<GenericMasterDefinition>
            findByTenantIdAndPlacementAndStatusAndActiveFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                    String tenantId,
                    GenericMasterPlacement placement,
                    GenericMasterStatus status
            );

    Optional<GenericMasterDefinition> findByIdAndTenantIdAndDeletedAtIsNull(
            Long id,
            String tenantId
    );

    Optional<GenericMasterDefinition>
            findByTenantIdAndMasterCodeAndStatusAndActiveFlagTrueAndDeletedAtIsNull(
                    String tenantId,
                    String masterCode,
                    GenericMasterStatus status
            );

    boolean existsByTenantIdAndMasterCodeAndDeletedAtIsNull(
            String tenantId,
            String masterCode
    );
}
