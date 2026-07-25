package com.project.backend.features.dailyreport.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.context.DailyReportCalculationContext;
import com.project.backend.features.dailyreport.dto.DailyReportAllowanceSaveRequest;
import com.project.backend.features.dailyreport.dto.DailyReportDeductionSaveRequest;
import com.project.backend.features.dailyreport.dto.DailyReportInputResponse;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.master.payrollitem.service.PayrollItemDailyInputService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyReportInputItemService {

    private final PayrollItemDailyInputService payrollItemDailyInputService;
    private final EmployeeRepository employeeRepository;
    private final EmployeeContractRepository employeeContractRepository;

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

        Map<String, Object> variables = buildVariables(request, contract);
        DailyReportCalculationContext context = DailyReportCalculationContext.builder()
                .employee(employee)
                .targetDate(request.workDate())
                .variables(variables)
                .build();

        return findItems(
                context,
                allowanceManualAmounts(request),
                deductionManualAmounts(request)
        );
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
            EmployeeContract contract
    ) {
        Map<String, Object> variables = new LinkedHashMap<>();

        putIfNotNull(variables, "workDate", request.workDate());
        putIfNotNull(variables, "paymentDate", request.paymentDate());
        putIfNotNull(variables, "customerId", request.customerId());
        putIfNotNull(variables, "customerSiteId", request.customerSiteId());
        putIfNotNull(variables, "jobCode", request.jobCode());
        putIfNotNull(variables, "siteRoleCode", request.siteRoleCode());
        putIfNotNull(variables, "startTime", request.startTime());
        putIfNotNull(variables, "endTime", request.endTime());
        putIfNotNull(variables, "breakMinutes", request.breakMinutes());
        putIfNotNull(variables, "workHours", request.workHours());
        putIfNotNull(variables, "overtimeHours", request.overtimeHours());
        putIfNotNull(variables, "nightWorkHours", request.nightWorkHours());
        putIfNotNull(variables, "holidayWorkHours", request.holidayWorkHours());
        putIfNotNull(variables, "vehicleUsedFlag", request.vehicleUsedFlag());
        putIfNotNull(variables, "mileage", request.mileage());
        putIfNotNull(variables, "paidLeaveDays", request.paidLeaveDays());

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
            if (item.deductionMasterId() != null) {
                putManualAmount(amounts, item.deductionMasterId(), item.amount(), "控除");
            }
        }
        return amounts;
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
}
