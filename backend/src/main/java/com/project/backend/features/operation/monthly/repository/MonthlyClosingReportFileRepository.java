package com.project.backend.features.operation.monthly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.operation.monthly.entity.MonthlyClosingReportFile;

public interface MonthlyClosingReportFileRepository
        extends JpaRepository<MonthlyClosingReportFile, Long> {

    Optional<MonthlyClosingReportFile>
            findFirstByMonthlyClosingIdAndClosingVersionAndReportCodeAndDeletedAtIsNullOrderByIdDesc(
                    Long monthlyClosingId,
                    Integer closingVersion,
                    String reportCode);

    List<MonthlyClosingReportFile>
            findAllByTargetMonthAndClosingVersionAndDeletedAtIsNullOrderByTargetNameAscIdAsc(
                    String targetMonth,
                    Integer closingVersion);

    List<MonthlyClosingReportFile>
            findAllByTargetMonthAndClosingScopeAndDeletedAtIsNullOrderByIdDesc(
                    String targetMonth,
                    String closingScope
            );

    List<MonthlyClosingReportFile>
            findByTenantIdAndTargetMonthBetweenAndDeletedAtIsNullOrderByTargetMonthAscIdAsc(
                    String tenantId,
                    String periodStartMonth,
                    String periodEndMonth
            );

    List<MonthlyClosingReportFile>
            findByTenantIdAndDeletedAtIsNullOrderByTargetMonthAscIdAsc(
                    String tenantId
            );
}
