package com.project.backend.features.system.report.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.report.entity.ReportOutputFile;
import com.project.backend.features.system.report.enums.ReportOutputFileStatus;

public interface ReportOutputFileRepository extends JpaRepository<ReportOutputFile, Long> {

    List<ReportOutputFile> findByExecutionIdAndDeletedAtIsNullOrderByIdAsc(String executionId);

    List<ReportOutputFile> findByMailTypeAndStatusAndDeletedAtIsNullOrderByIdAsc(
            String mailType,
            ReportOutputFileStatus status
    );

    List<ReportOutputFile> findByReportCodeAndStatusAndDeletedAtIsNullOrderByIdAsc(
            String reportCode,
            ReportOutputFileStatus status
    );

    boolean existsByBusinessKeyAndDeletedAtIsNull(String businessKey);

    boolean existsByBusinessKeyAndStatusInAndDeletedAtIsNull(
            String businessKey,
            List<ReportOutputFileStatus> statuses
    );
}