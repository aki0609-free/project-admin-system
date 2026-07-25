package com.project.backend.features.system.batch.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.batch.entity.BatchExecutionLog;

public interface BatchExecutionLogRepository extends JpaRepository<BatchExecutionLog, Long> {

    List<BatchExecutionLog> findTop200ByTenantIdAndDeletedAtIsNullOrderByIdDesc(
            String tenantId
    );

    List<BatchExecutionLog> findTop200ByTenantIdAndJobCodeAndDeletedAtIsNullOrderByIdDesc(
            String tenantId,
            String jobCode
    );

    Optional<BatchExecutionLog> findByIdAndTenantIdAndDeletedAtIsNull(
            Long id,
            String tenantId
    );
}
