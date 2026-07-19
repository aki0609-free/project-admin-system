package com.project.backend.features.dailyreport.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.dailyreport.entity.DailyReport;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

        List<DailyReport> findAllByDeletedAtIsNullOrderByWorkDateDescIdDesc();

        Optional<DailyReport> findByIdAndDeletedAtIsNull(Long id);

        List<DailyReport> findByWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        LocalDate from,
                        LocalDate to);

        List<DailyReport> findByEmployeeIdAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        Long employeeId);

        List<DailyReport> findByWorkDateAndDeletedAtIsNullOrderByEmployeeEmployeeCodeAscIdAsc(
                        LocalDate workDate);

        List<DailyReport> findByPaymentDateAndDeletedAtIsNullOrderByEmployeeEmployeeCodeAscIdAsc(
                        LocalDate paymentDate);

        boolean existsByEmployeeIdAndWorkDateAndDeletedAtIsNull(
                        Long employeeId,
                        LocalDate workDate);

        List<DailyReport> findByPaymentDateAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        LocalDate paymentDate);

}