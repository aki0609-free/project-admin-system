package com.project.backend.features.operation.monthly.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.monthly.entity.AnnualReportBackupExecution;

public interface AnnualReportBackupExecutionRepository
        extends JpaRepository<AnnualReportBackupExecution, Long> {

    Optional<AnnualReportBackupExecution>
            findByTenantIdAndFiscalYearAndDeletedAtIsNull(
                    String tenantId,
                    Integer fiscalYear
            );
}
