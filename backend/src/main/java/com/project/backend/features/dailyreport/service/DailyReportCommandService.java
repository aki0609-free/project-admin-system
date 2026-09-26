package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportResponse;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.dto.DailyReportInputResponse;
import com.project.backend.features.dailyreport.dto.DailyReportAllowanceSaveRequest;
import com.project.backend.features.dailyreport.dto.DailyReportDeductionSaveRequest;
import com.project.backend.features.dailyreport.dto.DailyReportInputItemResponse;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.mapper.DailyReportMapper;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.employee.service.EmployeeFinanceBalanceCommandService;
import com.project.backend.features.employee.service.EmployeePaidLeaveBalanceCommandService;
import com.project.backend.features.employee.service.EmployeeWorkEligibilityPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyReportCommandService {

    private final DailyReportRepository repository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeContractRepository employeeContractRepository;
    private final DailyReportMapper mapper;
    private final DailyReportSaveValidator saveValidator;
    private final DailyReportCustomerSiteResolver customerSiteResolver;
    private final DailyReportPlacementService placementService;

    private final DailyReportAllowanceCommandService allowanceCommandService;
    private final DailyReportDeductionCommandService deductionCommandService;

    private final EmployeeFinanceBalanceCommandService financeBalanceCommandService;
    private final EmployeePaidLeaveBalanceCommandService paidLeaveBalanceCommandService;
    private final DailyReportEstimatedPayService estimatedPayService;
    private final DailyReportBillingRateService billingRateService;
    private final DailyReportInputItemService inputItemService;
    private final EmployeeWorkEligibilityPolicy workEligibilityPolicy;
    private final DailyReportWorkTimeCalculator workTimeCalculator;
    private final Clock clock;

    public DailyReportResponse create(
            DailyReportSaveRequest request
    ) {
        saveValidator.validateForCreate(request);

        Employee employee = findEmployee(
                request.employeeId()
        );
        EmployeeContract contract = findEmployeeContract(employee.getId());
        workEligibilityPolicy.verifyEligible(employee, contract, request.workDate());

        DailyReport entity = new DailyReport();

        mapper.applyRequest(
                request,
                entity,
                employee
        );
        applyCalculatedWorkTimes(entity, request);

        customerSiteResolver.applySnapshot(entity, request);
        placementService.applyAuthoritativePlacement(entity);

        billingRateService.applyBillingRate(
                entity
        );

        DailyReport saved =
                repository.save(entity);

        DailyReportInputResponse calculatedItems =
                inputItemService.calculate(request);

        applyCalculatedAmounts(saved, calculatedItems);

        estimatedPayService.applyEstimatedPay(saved, contract);
        saved = repository.save(saved);

        allowanceCommandService.replaceAll(
                saved.getId(),
                toAllowanceRequests(calculatedItems)
        );

        deductionCommandService.replaceAll(
                saved.getId(),
                toDeductionRequests(calculatedItems)
        );

        financeBalanceCommandService.applyDailyReportAmountDiff(
                employee.getId(),
                nvl(saved.getSavingAmount()),
                nvl(saved.getSavingWithdrawalAmount()),
                nvl(saved.getLoanRepaymentAmount()),
                saved.getId(),
                saved.getWorkDate()
        );
        paidLeaveBalanceCommandService.applyUsageDiff(
                employee.getId(),
                nvl(saved.getPaidLeaveDays())
        );

        return mapper.toResponse(saved);
    }

    public DailyReportResponse update(
            Long id,
            DailyReportSaveRequest request
    ) {
        saveValidator.validateForUpdate(id, request);

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

        BigDecimal oldSavingWithdrawalAmount =
                nvl(entity.getSavingWithdrawalAmount());

        BigDecimal oldLoanRepaymentAmount =
                nvl(entity.getLoanRepaymentAmount());

        BigDecimal oldPaidLeaveDays =
                nvl(entity.getPaidLeaveDays());

        var oldWorkDate = entity.getWorkDate();

        Employee employee =
                findEmployee(request.employeeId());
        EmployeeContract contract = findEmployeeContract(employee.getId());
        workEligibilityPolicy.verifyEligible(employee, contract, request.workDate());

        mapper.applyRequest(
                request,
                entity,
                employee
        );
        applyCalculatedWorkTimes(entity, request);

        customerSiteResolver.applySnapshot(entity, request);
        placementService.applyAuthoritativePlacement(entity);

        billingRateService.applyBillingRate(
                entity
        );

        DailyReport saved =
                repository.save(entity);

        DailyReportInputResponse calculatedItems =
                inputItemService.calculate(request);

        applyCalculatedAmounts(saved, calculatedItems);

        estimatedPayService.applyEstimatedPay(saved, contract);
        saved = repository.save(saved);

        allowanceCommandService.replaceAll(
                saved.getId(),
                toAllowanceRequests(calculatedItems)
        );

        deductionCommandService.replaceAll(
                saved.getId(),
                toDeductionRequests(calculatedItems)
        );

        Long newEmployeeId =
                employee.getId();

        BigDecimal newSavingAmount =
                nvl(saved.getSavingAmount());

        BigDecimal newSavingWithdrawalAmount =
                nvl(saved.getSavingWithdrawalAmount());

        BigDecimal newLoanRepaymentAmount =
                nvl(saved.getLoanRepaymentAmount());

        if (oldEmployeeId.equals(newEmployeeId)) {
            financeBalanceCommandService.applyDailyReportAmountDiff(
                    newEmployeeId,
                    newSavingAmount.subtract(oldSavingAmount),
                    newSavingWithdrawalAmount.subtract(oldSavingWithdrawalAmount),
                    newLoanRepaymentAmount.subtract(
                            oldLoanRepaymentAmount
                    ),
                    saved.getId(),
                    saved.getWorkDate()
            );
            paidLeaveBalanceCommandService.applyUsageDiff(
                    newEmployeeId,
                    nvl(saved.getPaidLeaveDays()).subtract(oldPaidLeaveDays)
            );
        } else {
            financeBalanceCommandService.applyDailyReportAmountDiff(
                    oldEmployeeId,
                    oldSavingAmount.negate(),
                    oldSavingWithdrawalAmount.negate(),
                    oldLoanRepaymentAmount.negate(),
                    saved.getId(),
                    oldWorkDate
            );
            paidLeaveBalanceCommandService.applyUsageDiff(
                    oldEmployeeId,
                    oldPaidLeaveDays.negate()
            );

            financeBalanceCommandService.applyDailyReportAmountDiff(
                    newEmployeeId,
                    newSavingAmount,
                    newSavingWithdrawalAmount,
                    newLoanRepaymentAmount,
                    saved.getId(),
                    saved.getWorkDate()
            );
            paidLeaveBalanceCommandService.applyUsageDiff(
                    newEmployeeId,
                    nvl(saved.getPaidLeaveDays())
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
                nvl(entity.getSavingWithdrawalAmount()).negate(),
                nvl(entity.getLoanRepaymentAmount()).negate(),
                entity.getId(),
                entity.getWorkDate()
        );
        paidLeaveBalanceCommandService.applyUsageDiff(
                entity.getEmployee().getId(),
                nvl(entity.getPaidLeaveDays()).negate()
        );

        entity.setDeletedAt(
                Instant.now(clock)
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

    private BigDecimal nvl(
            BigDecimal value
    ) {
        return value != null
                ? value
                : BigDecimal.ZERO;
    }

    private void applyCalculatedAmounts(
            DailyReport report,
            DailyReportInputResponse calculatedItems
    ) {
        report.setAllowanceAmount(sumAmounts(calculatedItems.allowances()));
        report.setDeductionAmount(sumAmounts(calculatedItems.deductions()));
    }

    private void applyCalculatedWorkTimes(
            DailyReport report,
            DailyReportSaveRequest request
    ) {
        DailyReportWorkTimePolicy.WorkTimes times =
                workTimeCalculator.resolveForSave(request);
        report.setWorkHours(times.workHours());
        report.setOvertimeHours(times.overtimeHours());
        report.setNightWorkHours(times.nightWorkHours());
        report.setHolidayWorkHours(times.holidayWorkHours());
    }

    private BigDecimal sumAmounts(
            java.util.List<DailyReportInputItemResponse> items
    ) {
        return items.stream()
                .map(item -> BigDecimal.valueOf(item.amount() == null ? 0 : item.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private java.util.List<DailyReportAllowanceSaveRequest> toAllowanceRequests(
            DailyReportInputResponse calculatedItems
    ) {
        return calculatedItems.allowances().stream()
                .map(item -> new DailyReportAllowanceSaveRequest(
                        item.masterId(),
                        item.code(),
                        item.name(),
                        item.calculatedAmount(),
                        item.amount(),
                        item.manualOverride(),
                        item.overrideReason(),
                        item.quantity(),
                        item.balanceUnit()
                ))
                .toList();
    }

    private java.util.List<DailyReportDeductionSaveRequest> toDeductionRequests(
            DailyReportInputResponse calculatedItems
    ) {
        return calculatedItems.deductions().stream()
                .map(item -> new DailyReportDeductionSaveRequest(
                        item.masterId(),
                        item.code(),
                        item.name(),
                        item.calculatedAmount(),
                        item.amount(),
                        item.manualOverride(),
                        item.overrideReason(),
                        item.quantity(),
                        item.balanceUnit()
                ))
                .toList();
    }
}
