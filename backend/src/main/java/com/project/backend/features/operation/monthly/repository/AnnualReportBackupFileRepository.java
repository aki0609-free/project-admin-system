package com.project.backend.features.operation.monthly.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.monthly.entity.AnnualReportBackupFile;

public interface AnnualReportBackupFileRepository
        extends JpaRepository<AnnualReportBackupFile, Long> {

    List<AnnualReportBackupFile>
            findByTenantIdAndBackupExecutionIdAndDeletedAtIsNullOrderByIdAsc(
                    String tenantId,
                    Long backupExecutionId
            );

    boolean existsByTenantIdAndMonthlyClosingReportFileIdAndDeletedAtIsNull(
            String tenantId,
            Long monthlyClosingReportFileId
    );
}
