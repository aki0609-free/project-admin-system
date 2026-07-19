package com.project.backend.features.system.report.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.report.entity.ReportHistory;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {
    
    Integer countByReportMasterId(Long reportId);

    List<ReportHistory> findAllByOrderByIdDesc();

    List<ReportHistory> findByReportMasterIdOrderByIdDesc(Long reportMasterId);
}
