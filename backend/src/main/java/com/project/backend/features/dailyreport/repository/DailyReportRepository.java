package com.project.backend.features.dailyreport.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.employee.enums.ApprovalStatus;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

        List<DailyReport> findAllByDeletedAtIsNullOrderByWorkDateDescIdDesc();

        List<DailyReport> findAllByDeletedAtIsNullOrderByWorkDateDescPaymentDateAscIdDesc();

        Optional<DailyReport> findByIdAndDeletedAtIsNull(Long id);

        List<DailyReport> findByWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        LocalDate from,
                        LocalDate to);

        List<DailyReport> findByWorkDateBetweenAndApprovalStatusAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        LocalDate from,
                        LocalDate to,
                        ApprovalStatus approvalStatus);

        List<DailyReport> findByWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateDescPaymentDateAscIdDesc(
                        LocalDate from,
                        LocalDate to);

        List<DailyReport> findByEmployeeIdAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        Long employeeId);

        List<DailyReport> findByEmployeeIdAndDeletedAtIsNullOrderByWorkDateDescPaymentDateAscIdDesc(
                        Long employeeId);

        List<DailyReport> findByEmployeeIdAndWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateAscIdAsc(
                        Long employeeId,
                        LocalDate from,
                        LocalDate to);

        List<DailyReport> findByEmployeeIdAndWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        Long employeeId,
                        LocalDate from,
                        LocalDate to);

        List<DailyReport> findByEmployeeIdAndWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateDescPaymentDateAscIdDesc(
                        Long employeeId,
                        LocalDate from,
                        LocalDate to);

        List<DailyReport> findByWorkDateAndDeletedAtIsNullOrderByEmployeeEmployeeCodeAscIdAsc(
                        LocalDate workDate);

        List<DailyReport> findByPaymentDateAndDeletedAtIsNullOrderByEmployeeEmployeeCodeAscIdAsc(
                        LocalDate paymentDate);

        boolean existsByEmployeeIdAndWorkDateAndDeletedAtIsNull(
                        Long employeeId,
                        LocalDate workDate);

        Optional<DailyReport> findByEmployeeIdAndWorkDateAndDeletedAtIsNull(
                        Long employeeId,
                        LocalDate workDate);

        boolean existsByEmployeeIdAndWorkDateAndIdNotAndDeletedAtIsNull(
                        Long employeeId,
                        LocalDate workDate,
                        Long id);

        boolean existsByEmployeeIdAndDeletedAtIsNull(Long employeeId);

        boolean existsByCustomerIdAndDeletedAtIsNull(Long customerId);

        boolean existsByCustomerSiteIdAndDeletedAtIsNull(Long customerSiteId);

        List<DailyReport> findByPaymentDateAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        LocalDate paymentDate);

        List<DailyReport> findByPaymentDateAndApprovalStatusAndDeletedAtIsNullOrderByEmployeeEmployeeCodeAscWorkDateDescIdDesc(
                        LocalDate paymentDate,
                        ApprovalStatus approvalStatus);

        List<DailyReport> findByPaymentDateBetweenAndApprovalStatusAndDeletedAtIsNullOrderByPaymentDateAscEmployeeEmployeeCodeAscIdAsc(
                        LocalDate from,
                        LocalDate to,
                        ApprovalStatus approvalStatus);

}
