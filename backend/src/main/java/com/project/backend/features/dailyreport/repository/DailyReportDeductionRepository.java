package com.project.backend.features.dailyreport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.dailyreport.entity.DailyReportDeduction;

public interface DailyReportDeductionRepository
        extends JpaRepository<DailyReportDeduction, Long> {

    List<DailyReportDeduction> findByDailyReportIdOrderByIdAsc(Long dailyReportId);

    void deleteByDailyReportId(Long dailyReportId);
}