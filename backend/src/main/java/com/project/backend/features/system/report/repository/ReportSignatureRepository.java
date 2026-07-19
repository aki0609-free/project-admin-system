package com.project.backend.features.system.report.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.report.entity.ReportSignature;

public interface ReportSignatureRepository extends JpaRepository<ReportSignature, Long> {
    
    List<ReportSignature> findByReportMasterIdOrderByDisplayOrderAscIdAsc(Long reportMasterId);

    List<ReportSignature> findByReportMasterIdAndActiveFlagTrueOrderByDisplayOrderAscIdAsc(Long reportMasterId);

    Optional<ReportSignature> findFirstByReportMasterIdAndActiveFlagTrueAndDefaultFlagTrueOrderByDisplayOrderAscIdAsc(
        Long reportMasterId
    );

    boolean existsByReportMasterIdAndId(Long reportMasterId, Long id);

    List<ReportSignature> findByReportMasterIdAndIdNot(Long reportMasterId, Long id);

    List<ReportSignature> findByReportMasterId(Long reportMasterId);

    List<ReportSignature> findAllByOrderByIdAsc();

}
