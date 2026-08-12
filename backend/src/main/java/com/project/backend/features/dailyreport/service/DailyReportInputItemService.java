package com.project.backend.features.dailyreport.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.context.DailyReportCalculationContext;
import com.project.backend.features.dailyreport.dto.DailyReportAllowanceSaveRequest;
import com.project.backend.features.dailyreport.dto.DailyReportDeductionSaveRequest;
import com.project.backend.features.dailyreport.dto.DailyReportInputResponse;
import com.project.backend.features.dailyreport.dto.DailyReportInputItemResponse;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.admin.business.repository.DormitoryFeeSettingRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.master.payrollitem.service.PayrollItemDailyInputService;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalanceQueryService;
import com.project.backend.features.master.payrollitem.balance.EmployeePayrollItemSettingService;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyReportInputItemService {

    private final PayrollItemDailyInputService payrollItemDailyInputService;
    private final EmployeeRepository employeeRepository;
    private final EmployeeContractRepository employeeContractRepository;
    private final DormitoryFeeSettingRepository dormitoryFeeSettingRepository;
    private final PayrollItemBalanceQueryService balanceQueryService;
    private final EmployeePayrollItemSettingService payrollItemSettingService;
    private final DailyReportRepository dailyReportRepository;

    public DailyReportInputResponse findItems() {
        return findItems(
                DailyReportCalculationContext.builder()
                        .build(),
                Map.of(),
                Map.of()
        );
    }

    public DailyReportInputResponse preview(DailyReportSaveRequest request) {
        return calculate(request);
    }

    public DailyReportInputResponse calculate(
            DailyReportSaveRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("日報入力項目の計算リクエストは必須です。");
        }
        if (request.employeeId() == null) {
            throw new IllegalArgumentException("employeeId は必須です。");
        }
        if (request.workDate() == null) {
            throw new IllegalArgumentException("workDate は必須です。");
        }

        Employee employee = employeeRepository
                .findByIdAndDeletedAtIsNull(request.employeeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "従業員が見つかりません。employeeId=" + request.employeeId()
                ));

        EmployeeContract contract = employeeContractRepository
                .findByEmployeeIdAndDeletedAtIsNull(employee.getId())
                .orElse(null);

        Map<String, Object> variables = buildVariables(request, contract, employee);
        DailyReportCalculationContext context = DailyReportCalculationContext.builder()
                .employee(employee)
                .targetDate(request.workDate())
                .variables(variables)
                .build();

        DailyReportInputResponse calculated = findItems(
                context,
                allowanceManualAmounts(request),
                deductionManualAmounts(request)
        );
        return enrichBalances(calculated, request);
    }

    private DailyReportInputResponse findItems(
            DailyReportCalculationContext context,
            Map<Long, Integer> allowanceManualAmounts,
            Map<Long, Integer> deductionManualAmounts
    ) {
        return DailyReportInputResponse.builder()
                .allowances(
                        payrollItemDailyInputService.findAllowanceItems(
                                context.toParameters(),
                                allowanceManualAmounts
                        )
                )
                .deductions(
                        payrollItemDailyInputService.findDeductionItems(
                                context.toParameters(),
                                deductionManualAmounts
                        )
                )
                .build();
    }

    private Map<String, Object> buildVariables(
            DailyReportSaveRequest request,
            EmployeeContract contract,
            Employee employee
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();
        DailyReportWorkTimePolicy.WorkTimes workTimes =
                DailyReportWorkTimePolicy.resolve(
                        request.workDate(),
                        request.workHours(),
                        request.overtimeHours(),
                        request.nightWorkHours(),
                        request.holidayWorkHours(),
                        Boolean.TRUE.equals(request.holidayPremiumEligible())
                );

        putIfNotNull(variables, "workDate", request.workDate());
        putIfNotNull(variables, "paymentDate", request.paymentDate());
        putIfNotNull(variables, "customerId", request.customerId());
        putIfNotNull(variables, "customerSiteId", request.customerSiteId());
        putIfNotNull(variables, "jobCode", request.jobCode());
        putIfNotNull(variables, "siteRoleCode", request.siteRoleCode());
        putIfNotNull(variables, "startTime", request.startTime());
        putIfNotNull(variables, "endTime", request.endTime());
        putIfNotNull(variables, "breakMinutes", request.breakMinutes());
        putIfNotNull(variables, "workHours", workTimes.workHours());
        putIfNotNull(variables, "overtimeHours", workTimes.overtimeHours());
        putIfNotNull(variables, "nightWorkHours", workTimes.nightWorkHours());
        putIfNotNull(variables, "holidayWorkHours", workTimes.holidayWorkHours());
        putIfNotNull(variables, "vehicleUsedFlag", request.vehicleUsedFlag());
        putIfNotNull(variables, "mileage", request.mileage());
        putIfNotNull(variables, "paidLeaveDays", request.paidLeaveDays());
        putIfNotNull(
                variables,
                "dormitoryChargeDays",
                resolveDormitoryChargeDays(request)
        );
        variables.put("dormitoryDailyAmount", java.math.BigDecimal.ZERO);

        if (contract != null) {
            putIfNotNull(variables, "salaryType", contract.getSalaryType());
            putIfNotNull(variables, "hourlyWage", contract.getHourlyWage());
            putIfNotNull(variables, "dailyWage", contract.getDailyWage());
            putIfNotNull(variables, "weeklyWage", contract.getWeeklyWage());
            putIfNotNull(variables, "monthlySalary", contract.getMonthlySalary());
            putIfNotNull(
                    variables,
                    "standardWorkingHours",
                    contract.getStandardWorkingHours()
            );
        }

        if (employee != null && employee.isDormitoryFlag()) {
            java.util.Optional.ofNullable(employee.getDormitoryType())
                    .flatMap(dormitoryFeeSettingRepository
                            ::findByDormitoryTypeAndActiveFlagTrueAndDeletedAtIsNull)
                    .ifPresent(setting -> putIfNotNull(
                            variables,
                            "dormitoryDailyAmount",
                            setting.getDailyAmount()
                    ));
        }

        return variables;
    }

    private Map<Long, Integer> allowanceManualAmounts(DailyReportSaveRequest request) {
        Map<Long, Integer> amounts = new LinkedHashMap<>();
        for (DailyReportAllowanceSaveRequest item : request.allowances()) {
            if (item.allowanceMasterId() != null) {
                putManualAmount(amounts, item.allowanceMasterId(), item.amount(), "手当");
            }
        }
        return amounts;
    }

    private Map<Long, Integer> deductionManualAmounts(DailyReportSaveRequest request) {
        Map<Long, Integer> amounts = new LinkedHashMap<>();
        for (DailyReportDeductionSaveRequest item : request.deductions()) {
            if (item.deductionMasterId() != null
                    && Boolean.TRUE.equals(item.manualOverride())) {
                putManualAmount(amounts, item.deductionMasterId(), item.amount(), "控除");
            }
        }
        return amounts;
    }

    private DailyReportInputResponse enrichBalances(
            DailyReportInputResponse calculated,
            DailyReportSaveRequest request
    ) {
        Long existingId = dailyReportRepository
                .findByEmployeeIdAndWorkDateAndDeletedAtIsNull(
                        request.employeeId(), request.workDate()
                )
                .map(report -> report.getId())
                .orElse(null);

        var deductions = calculated.deductions().stream()
                .map(item -> enrichDeduction(item, request, existingId))
                .filter(item -> payrollItemSettingService.isDailyReportInputEnabled(
                        request.employeeId(), item.masterId(), request.workDate()
                ))
                .toList();
        return DailyReportInputResponse.builder()
                .allowances(calculated.allowances())
                .deductions(deductions)
                .build();
    }

    private DailyReportInputItemResponse enrichDeduction(
            DailyReportInputItemResponse item,
            DailyReportSaveRequest request,
            Long existingId
    ) {
        DailyReportDeductionSaveRequest submitted = request.deductions().stream()
                .filter(candidate -> item.masterId().equals(candidate.deductionMasterId()))
                .findFirst()
                .orElse(null);
        var balance = balanceQueryService.findDeductionBalance(
                request.employeeId(), item.masterId(), request.workDate(), existingId
        );
        java.math.BigDecimal quantity = submitted != null && submitted.quantity() != null
                ? submitted.quantity()
                : ("DORMITORY_FEE".equals(item.code())
                ? java.math.BigDecimal.valueOf(
                        request.dormitoryChargeDays() == null ? 0 : request.dormitoryChargeDays())
                : java.math.BigDecimal.ZERO);
        if (quantity.signum() < 0) {
            throw new IllegalArgumentException("消化数量は0以上で指定してください。");
        }
        if (balance.tracked()
                && balance.unit() == com.project.backend.features.master.payrollitem.balance.BalanceUnit.DAYS
                && quantity.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(item.name() + "の日数は整数で指定してください。");
        }
        if (balance.tracked() && quantity.compareTo(balance.remainingQuantity()) > 0) {
            throw new IllegalArgumentException(
                    item.name() + "の消化数量が残数量を超えています。remaining="
                            + balance.remainingQuantity() + ", quantity=" + quantity
            );
        }

        boolean overridden = submitted != null
                && Boolean.TRUE.equals(submitted.manualOverride());
        int calculatedAmount = item.calculatedAmount() == null
                ? 0 : item.calculatedAmount();
        int amount = item.amount() == null ? 0 : item.amount();
        if (balance.tracked()
                && balance.unit() == com.project.backend.features.master.payrollitem.balance.BalanceUnit.DAYS
                && (item.inputMode() == com.project.backend.features.dailyreport.enums.DailyReportInputMode.FIXED
                || item.inputMode() == com.project.backend.features.dailyreport.enums.DailyReportInputMode.FIXED_WITH_OVERRIDE)) {
            calculatedAmount = java.math.BigDecimal.valueOf(calculatedAmount)
                    .multiply(quantity)
                    .intValueExact();
            if (!overridden) {
                amount = calculatedAmount;
            }
        }
        return DailyReportInputItemResponse.builder()
                .masterId(item.masterId())
                .code(item.code())
                .name(item.name())
                .itemType(item.itemType())
                .inputMode(item.inputMode())
                .calculatedAmount(calculatedAmount)
                .amount(amount)
                .manualOverride(overridden)
                .overrideReason(overridden ? submitted.overrideReason() : null)
                .editable(item.editable())
                .displayOrder(item.displayOrder())
                .balanceTracked(balance.tracked())
                .balanceUnit(balance.unit() == null ? null : balance.unit().name())
                .openingQuantity(balance.openingQuantity())
                .accruedQuantity(balance.accruedQuantity())
                .consumedQuantity(balance.consumedQuantity())
                .remainingQuantity(balance.remainingQuantity())
                .quantity(quantity)
                .remainingAfterQuantity(balance.remainingQuantity().subtract(quantity))
                .build();
    }

    private void putManualAmount(
            Map<Long, Integer> amounts,
            Long masterId,
            Integer amount,
            String label
    ) {
        if (amounts.putIfAbsent(masterId, amount == null ? 0 : amount) != null) {
            throw new IllegalArgumentException(
                    label + "マスターIDが重複しています。masterId=" + masterId
            );
        }
    }

    private void putIfNotNull(Map<String, Object> variables, String key, Object value) {
        if (value != null) {
            variables.put(key, value);
        }
    }

    private int resolveDormitoryChargeDays(DailyReportSaveRequest request) {
        return request.deductions().stream()
                .filter(item -> "DORMITORY_FEE".equals(item.deductionCode()))
                .map(DailyReportDeductionSaveRequest::quantity)
                .filter(java.util.Objects::nonNull)
                .map(java.math.BigDecimal::intValueExact)
                .findFirst()
                .orElse(request.dormitoryChargeDays() == null
                        ? 0 : request.dormitoryChargeDays());
    }
}
