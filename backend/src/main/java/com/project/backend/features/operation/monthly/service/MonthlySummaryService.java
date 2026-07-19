package com.project.backend.features.operation.monthly.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.operation.daily.entity.DailyPayment;
import com.project.backend.features.operation.daily.repository.DailyPaymentRepository;
import com.project.backend.features.operation.monthly.dto.MonthlyClosingSummaryResponse;
import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.mapper.MonthlyClosingMapper;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.operation.monthly.utils.MonthlyOperationDateUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlySummaryService {

    private final MonthlyClosingRepository monthlyClosingRepository;
    private final DailyReportRepository dailyReportRepository;
    private final DailyPaymentRepository dailyPaymentRepository;
    private final MonthlyClosingMapper mapper;

    public MonthlyClosingSummaryResponse findSummary(String targetMonthText) {
        YearMonth targetMonth =
                MonthlyOperationDateUtil.parseTargetMonth(targetMonthText);

        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();

        List<DailyReport> reports =
                dailyReportRepository
                        .findByWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                                monthStart,
                                monthEnd);

        List<DailyPayment> dailyPayments =
                dailyPaymentRepository
                        .findByPaymentDateBetweenAndDeletedAtIsNullOrderByPaymentDateAscEmployeeCodeAscIdAsc(
                                monthStart,
                                monthEnd);

        Set<Long> employeeIds = new HashSet<>();

        BigDecimal totalGrossAmount = BigDecimal.ZERO;
        BigDecimal totalDeductionAmount = BigDecimal.ZERO;
        BigDecimal totalDailyPaymentAmount = BigDecimal.ZERO;

        for (DailyReport report : reports) {
            if (report.getEmployee() != null) {
                employeeIds.add(report.getEmployee().getId());
            }

            BigDecimal grossAmount = nvl(report.getEstimatedGrossPayAmount());

            if (grossAmount.compareTo(BigDecimal.ZERO) == 0) {
                grossAmount = nvl(report.getAllowanceAmount());
            }

            totalGrossAmount =
                    totalGrossAmount.add(grossAmount);

            totalDeductionAmount =
                    totalDeductionAmount
                            .add(nvl(report.getDeductionAmount()))
                            .add(nvl(report.getSavingAmount()))
                            .add(nvl(report.getLoanRepaymentAmount()));
        }

        for (DailyPayment payment : dailyPayments) {
            if (payment.getEmployeeId() != null) {
                employeeIds.add(payment.getEmployeeId());
            }

            totalDailyPaymentAmount =
                    totalDailyPaymentAmount.add(
                            nvl(payment.getActualAmount()));
        }

        BigDecimal totalNetPaymentAmount =
                totalGrossAmount
                        .subtract(totalDeductionAmount)
                        .subtract(totalDailyPaymentAmount);

        MonthlyClosing closing =
                monthlyClosingRepository
                        .findByTargetMonthAndDeletedAtIsNull(monthStart)
                        .orElse(null);

        return MonthlyClosingSummaryResponse.builder()
                .targetMonth(targetMonth.toString())
                .employeeCount(employeeIds.size())
                .workReportCount(reports.size())
                .totalGrossAmount(totalGrossAmount)
                .totalDeductionAmount(totalDeductionAmount)
                .totalDailyPaymentAmount(totalDailyPaymentAmount)
                .totalNetPaymentAmount(totalNetPaymentAmount)
                .closing(mapper.toResponse(closing))
                .build();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}