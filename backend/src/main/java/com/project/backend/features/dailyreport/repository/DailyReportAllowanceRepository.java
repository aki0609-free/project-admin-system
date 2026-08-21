package com.project.backend.features.dailyreport.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.backend.features.dailyreport.entity.DailyReportAllowance;

public interface DailyReportAllowanceRepository
        extends JpaRepository<DailyReportAllowance, Long> {

    List<DailyReportAllowance> findByDailyReportIdOrderByIdAsc(Long dailyReportId);

    void deleteByDailyReportId(Long dailyReportId);

    @Query(value = """
            SELECT COALESCE(SUM(item.quantity), 0)
            FROM daily_report_allowances item
            JOIN daily_report report ON report.id = item.daily_report_id
            WHERE item.tenant_id = :tenantId
              AND report.tenant_id = :tenantId
              AND item.allowance_master_id = :masterId
              AND report.employee_id = :employeeId
              AND report.work_date BETWEEN :from AND :through
              AND report.deleted_at IS NULL
              AND (:excludeDailyReportId IS NULL OR report.id <> :excludeDailyReportId)
            """, nativeQuery = true)
    BigDecimal sumQuantity(
            @Param("tenantId") String tenantId,
            @Param("employeeId") Long employeeId,
            @Param("masterId") Long masterId,
            @Param("from") LocalDate from,
            @Param("through") LocalDate through,
            @Param("excludeDailyReportId") Long excludeDailyReportId
    );
}
