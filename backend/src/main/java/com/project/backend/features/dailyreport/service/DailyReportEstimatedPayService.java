package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.project.backend.features.dailyreport.dto.DailyReportEstimatedPayPreviewResponse;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.SalaryType;

@Service
public class DailyReportEstimatedPayService {

    public void applyEstimatedPay(
            DailyReport report,
            EmployeeContract contract
    ) {
        DailyReportEstimatedPayPreviewResponse preview =
                calculatePreview(
                        contract,
                        report.getWorkHours(),
                        report.getOvertimeHours(),
                        report.getNightWorkHours(),
                        report.getAllowanceAmount(),
                        report.getDeductionAmount(),
                        report.getSavingAmount(),
                        report.getLoanRepaymentAmount()
                );

        report.setEstimatedGrossPayAmount(preview.estimatedGrossPayAmount());
        report.setEstimatedNetPayAmount(preview.estimatedNetPayAmount());
    }

    public DailyReportEstimatedPayPreviewResponse preview(
            DailyReportSaveRequest request,
            EmployeeContract contract
    ) {
        return calculatePreview(
                contract,
                request.workHours(),
                request.overtimeHours(),
                request.nightWorkHours(),
                request.allowanceAmount(),
                request.deductionAmount(),
                request.savingAmount(),
                request.loanRepaymentAmount()
        );
    }

    private DailyReportEstimatedPayPreviewResponse calculatePreview(
            EmployeeContract contract,
            BigDecimal workHours,
            BigDecimal overtimeHours,
            BigDecimal nightWorkHours,
            BigDecimal allowanceAmount,
            BigDecimal deductionAmount,
            BigDecimal savingAmount,
            BigDecimal loanRepaymentAmount
    ) {
        BigDecimal estimatedBasePayAmount =
                calculateBasePayAmount(
                        contract,
                        workHours,
                        overtimeHours,
                        nightWorkHours
                );

        BigDecimal estimatedGrossPayAmount =
                estimatedBasePayAmount.add(nvl(allowanceAmount));

        BigDecimal estimatedNetPayAmount =
                estimatedGrossPayAmount
                        .subtract(nvl(deductionAmount))
                        .subtract(nvl(savingAmount))
                        .subtract(nvl(loanRepaymentAmount));

        return DailyReportEstimatedPayPreviewResponse.builder()
                .estimatedBasePayAmount(estimatedBasePayAmount)
                .estimatedGrossPayAmount(estimatedGrossPayAmount)
                .estimatedNetPayAmount(estimatedNetPayAmount)
                .build();
    }

    private BigDecimal calculateBasePayAmount(
            EmployeeContract contract,
            BigDecimal workHours,
            BigDecimal overtimeHours,
            BigDecimal nightWorkHours
    ) {
        if (contract == null || contract.getSalaryType() == null) {
            return BigDecimal.ZERO;
        }

        SalaryType salaryType = contract.getSalaryType();

        return switch (salaryType) {
            case MONTHLY -> BigDecimal.ZERO;

            case WEEKLY -> calculateWeeklyBasePayAmount(
                    contract,
                    workHours,
                    overtimeHours,
                    nightWorkHours
            );

            case DAILY -> nvl(contract.getDailyWage());

            case HOURLY -> nvl(contract.getHourlyWage())
                    .multiply(
                            nvl(workHours)
                                    .add(nvl(overtimeHours))
                                    .add(nvl(nightWorkHours))
                    );
        };
    }

    private BigDecimal calculateWeeklyBasePayAmount(
            EmployeeContract contract,
            BigDecimal workHours,
            BigDecimal overtimeHours,
            BigDecimal nightWorkHours
    ) {
        BigDecimal weeklyWage = nvl(contract.getWeeklyWage());

        if (weeklyWage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalHours =
                nvl(workHours)
                        .add(nvl(overtimeHours))
                        .add(nvl(nightWorkHours));

        if (totalHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal standardWeeklyHours =
                contract.getStandardWorkingHours() != null
                        && contract.getStandardWorkingHours().compareTo(BigDecimal.ZERO) > 0
                                ? contract.getStandardWorkingHours()
                                : BigDecimal.valueOf(40);

        BigDecimal weekRate =
                totalHours.divide(
                        standardWeeklyHours,
                        4,
                        RoundingMode.HALF_UP
                );

        return weeklyWage.multiply(weekRate);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}