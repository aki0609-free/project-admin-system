package com.project.backend.features.system.settings.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.settings.entity.SystemSettingValue;

public interface SystemSettingValueRepository extends JpaRepository<SystemSettingValue, Long> {

    List<SystemSettingValue> findByTenantIdAndDefinitionIdInAndDeletedAtIsNull(
            String tenantId,
            Collection<Long> definitionIds
    );

    Optional<SystemSettingValue> findByTenantIdAndDefinitionIdAndDeletedAtIsNull(
            String tenantId,
            Long definitionId
    );

    Optional<SystemSettingValue> findByTenantIdAndDefinitionId(
            String tenantId,
            Long definitionId
    );

}
