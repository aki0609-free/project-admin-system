package com.project.backend.features.system.batch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.batch.entity.BatchJobDefinition;

public interface BatchJobDefinitionRepository extends JpaRepository<BatchJobDefinition, Long> {

    List<BatchJobDefinition> findAllByDeletedAtIsNullOrderByIdAsc();

    Optional<BatchJobDefinition> findByIdAndTenantIdAndDeletedAtIsNull(
            Long id,
            String tenantId
    );

    Optional<BatchJobDefinition> findByTenantIdAndJobCodeAndActiveFlagTrueAndDeletedAtIsNull(
            String tenantId,
            String jobCode
    );

    List<BatchJobDefinition> findAllByTenantIdAndDeletedAtIsNullOrderByIdAsc(
            String tenantId
    );

    boolean existsByTenantIdAndJobCodeAndDeletedAtIsNull(
            String tenantId,
            String jobCode
    );

    boolean existsByTenantIdAndJobCodeAndIdNotAndDeletedAtIsNull(
            String tenantId,
            String jobCode,
            Long id
    );
}
