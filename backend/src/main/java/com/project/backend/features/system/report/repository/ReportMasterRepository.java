package com.project.backend.features.system.report.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.features.system.report.entity.ReportMaster;

public interface ReportMasterRepository extends JpaRepository<ReportMaster, Long> {

    Optional<ReportMaster> findByReportCodeAndActiveFlagTrueAndDeletedAtIsNull(String reportCode);

    @Query("""
        SELECT r.fileName
        FROM ReportMaster r
        WHERE r.reportCode = :reportCode
          AND r.activeFlag = true
          AND r.deletedAt IS NULL
    """)
    Optional<String> findFileNameByReportCodeAndActiveFlagTrue(@Param("reportCode") String reportCode);

    boolean existsByReportCodeAndDeletedAtIsNull(String reportCode);

    boolean existsByReportCodeAndIdNotAndDeletedAtIsNull(String reportCode, Long id);

    List<ReportMaster> findByActiveFlagTrueAndDeletedAtIsNullOrderByIdAsc();

    @EntityGraph(attributePaths = { "params" })
    Optional<ReportMaster> findByIdAndDeletedAtIsNull(Long id);

    List<ReportMaster> findByDeletedAtIsNullOrderByIdAsc();

    Optional<ReportMaster> findByReportCodeAndDeletedAtIsNull(String reportCode);

    List<ReportMaster> findByDeletedAtIsNullOrderByIdDesc();
}