package com.project.backend.features.system.report.repository;

import java.util.List;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.features.system.report.entity.ReportHistory;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {
    
    Integer countByReportMasterId(Long reportId);

    List<ReportHistory> findAllByOrderByIdDesc();

    List<ReportHistory> findByReportMasterIdOrderByIdDesc(Long reportMasterId);

    @Modifying
    @Query("""
            UPDATE ReportHistory history
               SET history.deletedAt = :deletedAt
             WHERE history.tenantId = :tenantId
               AND history.storedFileKey IN :fileKeys
               AND history.deletedAt IS NULL
            """)
    int softDeleteByStoredFileKeys(
            @Param("tenantId") String tenantId,
            @Param("fileKeys") List<String> fileKeys,
            @Param("deletedAt") Instant deletedAt
    );
}
