package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

import com.project.backend.features.dailyreport.dto.DailyPayComponentAmounts;
import com.project.backend.features.dailyreport.dto.DailyReportEstimatedPayPreviewResponse;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.employee.entity.EmployeeContract;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyReportEstimatedPayService {

    private final DailyPayComponentCalculationService componentService;

    public void applyEstimatedPay(
            DailyReport report,
            EmployeeContract contract
    ) {
        DailyReportEstimatedPayPreviewResponse preview = calculatePreview(
                report,
                contract
        );

        report.setNormalPayAmount(preview.normalPayAmount());
        report.setOvertimePayAmount(preview.overtimePayAmount());
        report.setNightPayAmount(preview.nightPayAmount());
        report.setHolidayPayAmount(preview.holidayPayAmount());
        report.setEstimatedGrossPayAmount(preview.estimatedGrossPayAmount());
        report.setEstimatedNetPayAmount(preview.estimatedNetPayAmount());
    }

    public DailyReportEstimatedPayPreviewResponse preview(
            DailyReportSaveRequest request,
            EmployeeContract contract
    ) {
        DailyReport report = new DailyReport();
        report.setWorkDate(request.workDate());
        report.setPaymentDate(request.paymentDate());
        report.setCustomerId(request.customerId());
        report.setCustomerSiteId(request.customerSiteId());
        report.setJobCode(request.jobCode());
        report.setSiteRoleCode(request.siteRoleCode());
        report.setWorkHours(nvl(request.workHours()));
        report.setOvertimeHours(nvl(request.overtimeHours()));
        report.setNightWorkHours(nvl(request.nightWorkHours()));
        report.setHolidayWorkHours(nvl(request.holidayWorkHours()));
        report.setMileage(nvl(request.mileage()));
        report.setAllowanceAmount(nvl(request.allowanceAmount()));
        report.setDeductionAmount(nvl(request.deductionAmount()));
        report.setSavingAmount(nvl(request.savingAmount()));
        report.setLoanRepaymentAmount(nvl(request.loanRepaymentAmount()));
        return calculatePreview(report, contract, request.employeeId());
    }

    private DailyReportEstimatedPayPreviewResponse calculatePreview(
            DailyReport report,
            EmployeeContract contract
    ) {
        Long employeeId = report.getEmployee() == null
                ? null
                : report.getEmployee().getId();
        return calculatePreview(report, contract, employeeId);
    }

    private DailyReportEstimatedPayPreviewResponse calculatePreview(
            DailyReport report,
            EmployeeContract contract,
            Long employeeId
    ) {
        DailyPayComponentAmounts components =
                componentService.calculate(report, contract, employeeId);
        BigDecimal estimatedBasePayAmount = components.total();

        BigDecimal estimatedGrossPayAmount =
                estimatedBasePayAmount.add(nvl(report.getAllowanceAmount()));

        BigDecimal estimatedNetPayAmount =
                estimatedGrossPayAmount
                        .subtract(nvl(report.getDeductionAmount()))
                        .subtract(nvl(report.getSavingAmount()))
                        .subtract(nvl(report.getLoanRepaymentAmount()));

        return DailyReportEstimatedPayPreviewResponse.builder()
                .estimatedBasePayAmount(estimatedBasePayAmount)
                .normalPayAmount(components.normalPayAmount())
                .overtimePayAmount(components.overtimePayAmount())
                .nightPayAmount(components.nightPayAmount())
                .holidayPayAmount(components.holidayPayAmount())
                .estimatedGrossPayAmount(estimatedGrossPayAmount)
                .estimatedNetPayAmount(estimatedNetPayAmount)
                .build();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
