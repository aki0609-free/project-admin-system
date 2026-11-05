package com.project.backend.features.system.masterdata.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.masterdata.entity.GenericMasterFieldDefinition;

public interface GenericMasterFieldDefinitionRepository
        extends JpaRepository<GenericMasterFieldDefinition, Long> {

    List<GenericMasterFieldDefinition>
            findByTenantIdAndDefinitionIdAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc(
                    String tenantId,
                    Long definitionId
            );
}
