package com.project.backend.features.system.report.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.system.report.entity.ReportTestParamTemplate;

public interface ReportTestParamTemplateRepository
        extends JpaRepository<ReportTestParamTemplate, Long> {

    List<ReportTestParamTemplate> findByReportCodeAndDeletedAtIsNullOrderByIdDesc(String reportCode);

    Optional<ReportTestParamTemplate> findFirstByReportCodeAndDefaultFlagTrueAndDeletedAtIsNullOrderByIdDesc(
            String reportCode
    );
}