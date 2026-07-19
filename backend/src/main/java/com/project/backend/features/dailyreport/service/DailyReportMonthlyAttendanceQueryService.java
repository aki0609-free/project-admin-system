package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.common.closing.service.ClosingSettingQueryService;
import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.dailyreport.dto.DailyReportMonthlyAttendanceResponse;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.entity.EmployeePayrollProfile;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeePayrollProfileRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyReportMonthlyAttendanceQueryService {

    private final DailyReportRepository dailyReportRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeContractRepository employeeContractRepository;
    private final EmployeePayrollProfileRepository payrollProfileRepository;
    private final ClosingSettingQueryService closingSettingQueryService;

    @SuppressWarnings("null")
public List<DailyReportMonthlyAttendanceResponse> findMonthlyAttendance(
            YearMonth targetMonth
    ) {
        if (targetMonth == null) {
            throw new RuntimeException("targetMonth は必須です。");
        }

        DayRule closingDay = closingSettingQueryService.getPayrollClosingDayRule();
        AttendancePeriod period = resolvePeriod(targetMonth, closingDay);

        List<DailyReport> reports = dailyReportRepository
                .findByWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        period.start(),
                        period.end()
                );

        Map<Long, List<DailyReport>> reportMap = reports.stream()
                .collect(Collectors.groupingBy(report -> report.getEmployee().getId()));

        return employeeRepository.findAllByDeletedAtIsNullOrderByIdAsc()
                .stream()
                .sorted(
                        Comparator
                                .comparing(Employee::getEmployeeCode, Comparator.nullsLast(String::compareTo))
                                .thenComparing(Employee::getId)
                )
                .map(employee -> {
                    EmployeeContract contract = employeeContractRepository
                            .findByEmployeeIdAndDeletedAtIsNull(employee.getId())
                            .orElse(null);

                    EmployeePayrollProfile payrollProfile = payrollProfileRepository
                            .findByEmployeeIdAndDeletedAtIsNull(employee.getId())
                            .orElse(null);

                    return toResponse(
                            employee,
                            contract,
                            payrollProfile,
                            targetMonth,
                            period,
                            reportMap.getOrDefault(employee.getId(), List.of())
                    );
                })
                .toList();
    }

    private AttendancePeriod resolvePeriod(
            YearMonth targetMonth,
            DayRule closingRule
    ) {
        if (closingRule == null || closingRule.type() == null) {
            throw new RuntimeException("締日設定が不正です。");
        }

        if (closingRule.type() == DayRuleType.END_OF_MONTH) {
            return new AttendancePeriod(
                    targetMonth.atDay(1),
                    targetMonth.atEndOfMonth()
            );
        }

        int closingDay = closingRule.value() != null
                ? closingRule.value()
                : 0;

        if (closingDay <= 0) {
            throw new RuntimeException("締日の日付が不正です。");
        }

        if (closingDay >= targetMonth.lengthOfMonth()) {
            return new AttendancePeriod(
                    targetMonth.atDay(1),
                    targetMonth.atEndOfMonth()
            );
        }

        YearMonth previousMonth = targetMonth.minusMonths(1);

        LocalDate start = previousMonth.atDay(
                Math.min(closingDay + 1, previousMonth.lengthOfMonth())
        );

        LocalDate end = targetMonth.atDay(closingDay);

        return new AttendancePeriod(start, end);
    }

    private DailyReportMonthlyAttendanceResponse toResponse(
            Employee employee,
            EmployeeContract contract,
            EmployeePayrollProfile payrollProfile,
            YearMonth targetMonth,
            AttendancePeriod period,
            List<DailyReport> reports
    ) {
        BigDecimal totalWorkHours = sum(reports, "workHours");
        BigDecimal totalOvertimeHours = sum(reports, "overtimeHours");
        BigDecimal totalNightWorkHours = sum(reports, "nightWorkHours");

        BigDecimal totalAllowanceAmount = sum(reports, "allowanceAmount");
        BigDecimal totalDeductionAmount = sum(reports, "deductionAmount");

        BigDecimal totalSavingAmount = sum(reports, "savingAmount");
        BigDecimal totalLoanRepaymentAmount = sum(reports, "loanRepaymentAmount");

        BigDecimal paidLeaveUsedDays = sum(reports, "paidLeaveDays");

        BigDecimal paidLeaveRemainingDays =
                payrollProfile != null && payrollProfile.getPaidLeaveRemainingDays() != null
                        ? payrollProfile.getPaidLeaveRemainingDays()
                        : BigDecimal.ZERO;

        BigDecimal paidLeaveRemainingAfterUsedDays =
                paidLeaveRemainingDays.subtract(paidLeaveUsedDays);

        SalaryType salaryType = contract != null
                ? contract.getSalaryType()
                : null;

        BigDecimal baseSalaryAmount = resolveBaseSalaryAmount(contract);
        BigDecimal grossSalaryAmount = calculateGrossSalaryAmount(
                contract,
                reports,
                totalWorkHours,
                totalOvertimeHours,
                totalNightWorkHours
        );

        BigDecimal estimatedPaymentAmount =
                grossSalaryAmount
                        .add(totalAllowanceAmount)
                        .subtract(totalDeductionAmount)
                        .subtract(totalSavingAmount)
                        .subtract(totalLoanRepaymentAmount);

        return DailyReportMonthlyAttendanceResponse.builder()
                .employeeId(employee.getId())
                .employeeCode(employee.getEmployeeCode())
                .employeeName(employee.getEmployeeName())

                .targetMonth(targetMonth)
                .periodStart(period.start())
                .periodEnd(period.end())

                .salaryType(salaryType)
                .baseSalaryAmount(baseSalaryAmount)
                .grossSalaryAmount(grossSalaryAmount)

                .reportCount(reports.size())

                .paidLeaveUsedDays(paidLeaveUsedDays)
                .paidLeaveRemainingDays(paidLeaveRemainingDays)
                .paidLeaveRemainingAfterUsedDays(paidLeaveRemainingAfterUsedDays)

                .totalWorkHours(totalWorkHours)
                .totalOvertimeHours(totalOvertimeHours)
                .totalNightWorkHours(totalNightWorkHours)

                .totalAllowanceAmount(totalAllowanceAmount)
                .totalDeductionAmount(totalDeductionAmount)

                .totalSavingAmount(totalSavingAmount)
                .totalLoanRepaymentAmount(totalLoanRepaymentAmount)

                .estimatedPaymentAmount(estimatedPaymentAmount)
                .build();
    }

    private BigDecimal resolveBaseSalaryAmount(EmployeeContract contract) {
        if (contract == null || contract.getSalaryType() == null) {
            return BigDecimal.ZERO;
        }

        return switch (contract.getSalaryType()) {
            case MONTHLY -> nvl(contract.getMonthlySalary());
            case WEEKLY -> nvl(contract.getWeeklyWage());
            case DAILY -> nvl(contract.getDailyWage());
            case HOURLY -> nvl(contract.getHourlyWage());
        };
    }

    private BigDecimal calculateGrossSalaryAmount(
            EmployeeContract contract,
            List<DailyReport> reports,
            BigDecimal totalWorkHours,
            BigDecimal totalOvertimeHours,
            BigDecimal totalNightWorkHours
    ) {
        if (contract == null || contract.getSalaryType() == null) {
            return BigDecimal.ZERO;
        }

        return switch (contract.getSalaryType()) {
            case MONTHLY -> nvl(contract.getMonthlySalary());

            case WEEKLY -> nvl(contract.getWeeklyWage())
                    .multiply(calculateWeekCount(reports));

            case DAILY -> nvl(contract.getDailyWage())
                    .multiply(BigDecimal.valueOf(reports.size()));

            case HOURLY -> nvl(contract.getHourlyWage())
                    .multiply(
                            nvl(totalWorkHours)
                                    .add(nvl(totalOvertimeHours))
                                    .add(nvl(totalNightWorkHours))
                    );
        };
    }

    private BigDecimal calculateWeekCount(List<DailyReport> reports) {
        @SuppressWarnings("null")
        long workDateCount = reports.stream()
                .map(DailyReport::getWorkDate)
                .distinct()
                .count();

        if (workDateCount <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(workDateCount)
                .divide(BigDecimal.valueOf(7), 2, java.math.RoundingMode.HALF_UP);
    }

    @SuppressWarnings("null")
private BigDecimal sum(
            List<DailyReport> reports,
            String fieldName
    ) {
        return reports.stream()
                .map(report -> switch (fieldName) {
                    case "workHours" -> report.getWorkHours();
                    case "overtimeHours" -> report.getOvertimeHours();
                    case "nightWorkHours" -> report.getNightWorkHours();
                    case "allowanceAmount" -> report.getAllowanceAmount();
                    case "deductionAmount" -> report.getDeductionAmount();
                    case "savingAmount" -> report.getSavingAmount();
                    case "loanRepaymentAmount" -> report.getLoanRepaymentAmount();
                    case "paidLeaveDays" -> report.getPaidLeaveDays();
                    default -> BigDecimal.ZERO;
                })
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record AttendancePeriod(
            LocalDate start,
            LocalDate end
    ) {
    }
}