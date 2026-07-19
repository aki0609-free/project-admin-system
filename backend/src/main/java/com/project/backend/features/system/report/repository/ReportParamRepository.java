package com.project.backend.features.system.report.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.report.entity.ReportParam;

public interface ReportParamRepository extends JpaRepository<ReportParam, Long> {
    
    List<ReportParam> findByReportMasterIdAndActiveFlagTrue(Long reportMasterId);

    List<ReportParam> findByReportMasterIdAndParamName(Long reportMasterId, String paramName);

    List<ReportParam> findByReportMasterIdAndActiveFlagTrueOrderByDisplayOrderAscIdAsc(Long reportMasterId);

    List<ReportParam> findAllByOrderByIdAsc();

    List<ReportParam> findByReportMasterIdOrderByDisplayOrderAscIdAsc(Long reportMasterId);

    boolean existsByReportMasterIdAndParamName(Long reportMasterId, String paramName);

    boolean existsByReportMasterIdAndParamNameAndIdNot(Long reportMasterId, String paramName, Long id);
}
