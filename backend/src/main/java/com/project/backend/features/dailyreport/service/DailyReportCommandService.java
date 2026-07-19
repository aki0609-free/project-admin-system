package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportResponse;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.mapper.DailyReportMapper;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.employee.service.EmployeeFinanceBalanceCommandService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyReportCommandService {

    private final DailyReportRepository repository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeContractRepository employeeContractRepository;
    private final DailyReportMapper mapper;

    private final DailyReportAllowanceCommandService allowanceCommandService;
    private final DailyReportDeductionCommandService deductionCommandService;

    private final EmployeeFinanceBalanceCommandService financeBalanceCommandService;
    private final DailyReportEstimatedPayService estimatedPayService;
    private final DailyReportBillingRateService billingRateService;

    public DailyReportResponse create(
            DailyReportSaveRequest request
    ) {
        validateRequest(request);

        Employee employee = findEmployee(
                request.employeeId()
        );

        DailyReport entity = new DailyReport();

        mapper.applyRequest(
                request,
                entity,
                employee
        );

        billingRateService.applyBillingRate(
                entity
        );

        EmployeeContract contract =
                findEmployeeContract(
                        employee.getId()
                );

        estimatedPayService.applyEstimatedPay(
                entity,
                contract
        );

        DailyReport saved =
                repository.save(entity);

        allowanceCommandService.replaceAll(
                saved.getId(),
                request.allowances()
        );

        deductionCommandService.replaceAll(
                saved.getId(),
                request.deductions()
        );

        financeBalanceCommandService.applyDailyReportAmountDiff(
                employee.getId(),
                nvl(saved.getSavingAmount()),
                nvl(saved.getLoanRepaymentAmount())
        );

        return mapper.toResponse(saved);
    }

    public DailyReportResponse update(
            Long id,
            DailyReportSaveRequest request
    ) {
        validateRequest(request);

        DailyReport entity =
                repository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "日報が見つかりません。 id=" + id
                                )
                        );

        Long oldEmployeeId =
                entity.getEmployee().getId();

        BigDecimal oldSavingAmount =
                nvl(entity.getSavingAmount());

        BigDecimal oldLoanRepaymentAmount =
                nvl(entity.getLoanRepaymentAmount());

        Employee employee =
                findEmployee(request.employeeId());

        mapper.applyRequest(
                request,
                entity,
                employee
        );

        billingRateService.applyBillingRate(
                entity
        );

        EmployeeContract contract =
                findEmployeeContract(
                        employee.getId()
                );

        estimatedPayService.applyEstimatedPay(
                entity,
                contract
        );

        DailyReport saved =
                repository.save(entity);

        allowanceCommandService.replaceAll(
                saved.getId(),
                request.allowances()
        );

        deductionCommandService.replaceAll(
                saved.getId(),
                request.deductions()
        );

        Long newEmployeeId =
                employee.getId();

        BigDecimal newSavingAmount =
                nvl(saved.getSavingAmount());

        BigDecimal newLoanRepaymentAmount =
                nvl(saved.getLoanRepaymentAmount());

        if (oldEmployeeId.equals(newEmployeeId)) {
            financeBalanceCommandService.applyDailyReportAmountDiff(
                    newEmployeeId,
                    newSavingAmount.subtract(oldSavingAmount),
                    newLoanRepaymentAmount.subtract(
                            oldLoanRepaymentAmount
                    )
            );
        } else {
            financeBalanceCommandService.applyDailyReportAmountDiff(
                    oldEmployeeId,
                    oldSavingAmount.negate(),
                    oldLoanRepaymentAmount.negate()
            );

            financeBalanceCommandService.applyDailyReportAmountDiff(
                    newEmployeeId,
                    newSavingAmount,
                    newLoanRepaymentAmount
            );
        }

        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        DailyReport entity =
                repository.findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "日報が見つかりません。 id=" + id
                                )
                        );

        financeBalanceCommandService.applyDailyReportAmountDiff(
                entity.getEmployee().getId(),
                nvl(entity.getSavingAmount()).negate(),
                nvl(entity.getLoanRepaymentAmount()).negate()
        );

        entity.setDeletedAt(
                Instant.now()
        );
    }

    private Employee findEmployee(
            Long employeeId
    ) {
        return employeeRepository
                .findByIdAndDeletedAtIsNull(
                        employeeId
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "従業員が見つかりません。 employeeId="
                                        + employeeId
                        )
                );
    }

    private EmployeeContract findEmployeeContract(
            Long employeeId
    ) {
        return employeeContractRepository
                .findByEmployeeIdAndDeletedAtIsNull(
                        employeeId
                )
                .orElse(null);
    }

    private void validateRequest(
            DailyReportSaveRequest request
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "リクエストが不正です。"
            );
        }

        if (request.employeeId() == null) {
            throw new RuntimeException(
                    "employeeId は必須です。"
            );
        }

        if (request.workDate() == null) {
            throw new RuntimeException(
                    "workDate は必須です。"
            );
        }

        if (request.customerSiteId() != null
                && (request.jobCode() == null
                || request.jobCode().isBlank())) {
            throw new RuntimeException(
                    "現場を指定した場合、jobCode は必須です。"
            );
        }

        validateNonNegative(
                request.workHours(),
                "workHours"
        );

        validateNonNegative(
                request.overtimeHours(),
                "overtimeHours"
        );

        validateNonNegative(
                request.nightWorkHours(),
                "nightWorkHours"
        );

        validateNonNegative(
                request.holidayWorkHours(),
                "holidayWorkHours"
        );

        validateNonNegative(
                request.paidLeaveDays(),
                "paidLeaveDays"
        );

        validateNonNegative(
                request.mileage(),
                "mileage"
        );

        /*
         * 通常時間と休日時間を同時に入力するケースは、
         * 現時点では禁止しない。
         *
         * 半日通常勤務＋半日休日勤務などを
         * 将来的に扱える余地を残すため。
         */
    }

    private void validateNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        if (value != null
                && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(
                    fieldName
                            + " は0以上で指定してください。"
            );
        }
    }

    private BigDecimal nvl(
            BigDecimal value
    ) {
        return value != null
                ? value
                : BigDecimal.ZERO;
    }
}