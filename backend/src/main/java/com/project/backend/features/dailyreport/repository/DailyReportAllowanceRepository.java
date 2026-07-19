package com.project.backend.features.dailyreport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.dailyreport.entity.DailyReportAllowance;

public interface DailyReportAllowanceRepository
        extends JpaRepository<DailyReportAllowance, Long> {

    List<DailyReportAllowance> findByDailyReportIdOrderByIdAsc(Long dailyReportId);

    void deleteByDailyReportId(Long dailyReportId);
}