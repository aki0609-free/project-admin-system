package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dailyreport.dto.DailyPayComponentAmounts;
import com.project.backend.features.dailyreport.entity.DailyPayRuleSetting;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.enums.DailyPayComponentType;
import com.project.backend.features.dailyreport.repository.DailyPayRuleSettingRepository;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.system.rule.dto.RuleContextRequest;
import com.project.backend.features.system.rule.dto.RuleExecutionResult;
import com.project.backend.features.system.rule.service.RuleExecutionService;

import lombok.RequiredArgsConstructor;

/**
 * 日報の給与本体をRuleで計算し、用途別の金額へ分離する。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPayComponentCalculationService {

    private final DailyPayRuleSettingRepository settingRepository;
    private final RuleExecutionService ruleExecutionService;

    public DailyPayComponentAmounts calculate(
            DailyReport report,
            EmployeeContract contract
    ) {
        Long employeeId = report.getEmployee() == null
                ? null
                : report.getEmployee().getId();
        return calculate(report, contract, employeeId);
    }

    public DailyPayComponentAmounts calculate(
            DailyReport report,
            EmployeeContract contract,
            Long employeeId
    ) {
        Map<String, Object> parameters = parameters(
                report,
                contract,
                employeeId
        );
        Map<DailyPayComponentType, String> ruleNames = ruleNames();

        BigDecimal normal = calculateOne(
                DailyPayComponentType.NORMAL_PAY,
                ruleNames,
                parameters,
                fallbackNormal(report, contract)
        );
        BigDecimal overtime = calculateOne(
                DailyPayComponentType.OVERTIME_PAY,
                ruleNames,
                parameters,
                fallbackHourlyComponent(
                        contract,
                        report.getOvertimeHours()
                )
        );
        BigDecimal night = calculateOne(
                DailyPayComponentType.NIGHT_PAY,
                ruleNames,
                parameters,
                fallbackHourlyComponent(
                        contract,
                        report.getNightWorkHours()
                )
        );
        BigDecimal holiday = calculateOne(
                DailyPayComponentType.HOLIDAY_PAY,
                ruleNames,
                parameters,
                fallbackHourlyComponent(
                        contract,
                        report.getHolidayWorkHours()
                )
        );
        return new DailyPayComponentAmounts(
                normal,
                overtime,
                night,
                holiday
        );
    }

    private BigDecimal calculateOne(
            DailyPayComponentType componentType,
            Map<DailyPayComponentType, String> ruleNames,
            Map<String, Object> baseParameters,
            BigDecimal fallback
    ) {
        String ruleName = ruleNames.get(componentType);
        if (ruleName == null || ruleName.isBlank()) {
            return money(fallback);
        }

        Map<String, Object> parameters = new LinkedHashMap<>(baseParameters);
        parameters.put("componentType", componentType.name());
        RuleExecutionResult result = ruleExecutionService.execute(
                ruleName,
                RuleContextRequest.builder()
                        .parameters(parameters)
                        .build()
        );
        BigDecimal amount = toBigDecimal(result.result());
        if (amount.signum() < 0) {
            throw new IllegalStateException(
                    "日報給与Ruleの計算結果は0以上である必要があります。componentType="
                            + componentType
            );
        }
        return money(amount);
    }

    private Map<DailyPayComponentType, String> ruleNames() {
        Map<DailyPayComponentType, String> result =
                new EnumMap<>(DailyPayComponentType.class);
        for (DailyPayRuleSetting setting : settingRepository
                .findByTenantIdAndActiveFlagTrueAndDeletedAtIsNull(
                        TenantContext.getTenantId()
                )) {
            if (result.putIfAbsent(
                    setting.getComponentType(),
                    setting.getRuleName()
            ) != null) {
                throw new IllegalStateException(
                        "日報給与Rule設定が重複しています。componentType="
                                + setting.getComponentType()
                );
            }
        }
        return result;
    }

    private Map<String, Object> parameters(
            DailyReport report,
            EmployeeContract contract,
            Long employeeId
    ) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        put(parameters, "dailyReportId", report.getId());
        put(parameters, "employeeId", employeeId);
        put(parameters, "targetDate", report.getWorkDate());
        put(parameters, "workDate", report.getWorkDate());
        put(parameters, "paymentDate", report.getPaymentDate());
        put(parameters, "workHours", report.getWorkHours());
        put(parameters, "overtimeHours", report.getOvertimeHours());
        put(parameters, "nightWorkHours", report.getNightWorkHours());
        put(parameters, "holidayWorkHours", report.getHolidayWorkHours());
        put(parameters, "customerId", report.getCustomerId());
        put(parameters, "customerSiteId", report.getCustomerSiteId());
        put(parameters, "jobCode", report.getJobCode());
        put(parameters, "siteRoleCode", report.getSiteRoleCode());
        put(parameters, "mileage", report.getMileage());

        if (contract != null) {
            put(parameters, "salaryType", contract.getSalaryType());
            put(parameters, "hourlyWage", contract.getHourlyWage());
            put(parameters, "dailyWage", contract.getDailyWage());
            put(parameters, "weeklyWage", contract.getWeeklyWage());
            put(parameters, "monthlySalary", contract.getMonthlySalary());
            put(parameters, "standardWorkingHours",
                    contract.getStandardWorkingHours());
            put(parameters, "calculationHourlyRate",
                    calculationHourlyRate(contract));
        }
        return parameters;
    }

    private BigDecimal fallbackNormal(
            DailyReport report,
            EmployeeContract contract
    ) {
        if (contract == null || contract.getSalaryType() == null) {
            return BigDecimal.ZERO;
        }
        return switch (contract.getSalaryType()) {
            /*
             * 月給そのものは月次給与計算で使用する。
             * 日報・月間労務表には、年平均の時間単価へ換算した
             * 日次配賦額を保存する。
             */
            case MONTHLY -> calculationHourlyRate(contract)
                    .multiply(nvl(report.getWorkHours()));
            case DAILY -> nvl(contract.getDailyWage());
            case HOURLY -> nvl(contract.getHourlyWage())
                    .multiply(nvl(report.getWorkHours()));
            case WEEKLY -> weeklyComponent(
                    contract,
                    report.getWorkHours()
            );
        };
    }

    private BigDecimal fallbackHourlyComponent(
            EmployeeContract contract,
            BigDecimal hours
    ) {
        if (contract == null || contract.getSalaryType() == null) {
            return BigDecimal.ZERO;
        }
        if (contract.getSalaryType() == SalaryType.HOURLY) {
            return nvl(contract.getHourlyWage()).multiply(nvl(hours));
        }
        if (contract.getSalaryType() == SalaryType.WEEKLY) {
            return weeklyComponent(contract, hours);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal weeklyComponent(
            EmployeeContract contract,
            BigDecimal hours
    ) {
        BigDecimal standardHours =
                nvl(contract.getStandardWorkingHours()).signum() > 0
                        ? contract.getStandardWorkingHours()
                        : BigDecimal.valueOf(40);
        return nvl(contract.getWeeklyWage())
                .multiply(nvl(hours))
                .divide(standardHours, 4, RoundingMode.HALF_UP);
    }

    /**
     * Ruleからも使用できる給与計算用の基礎時給。
     *
     * <p>MONTHLYは「月給 × 12 ÷（週所定労働時間 × 52）」で、
     * 年平均の月所定労働時間による換算と同じ結果になる。</p>
     */
    private BigDecimal calculationHourlyRate(
            EmployeeContract contract
    ) {
        if (contract == null || contract.getSalaryType() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal standardWeeklyHours =
                nvl(contract.getStandardWorkingHours()).signum() > 0
                        ? contract.getStandardWorkingHours()
                        : BigDecimal.valueOf(40);

        return switch (contract.getSalaryType()) {
            case MONTHLY -> nvl(contract.getMonthlySalary())
                    .multiply(BigDecimal.valueOf(12))
                    .divide(
                            standardWeeklyHours
                                    .multiply(BigDecimal.valueOf(52)),
                            8,
                            RoundingMode.HALF_UP
                    );
            case WEEKLY -> nvl(contract.getWeeklyWage())
                    .divide(
                            standardWeeklyHours,
                            8,
                            RoundingMode.HALF_UP
                    );
            case HOURLY -> nvl(contract.getHourlyWage());
            case DAILY -> BigDecimal.ZERO;
        };
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "日報給与Ruleの計算結果が数値ではありません。value=" + value,
                    exception
            );
        }
    }

    private BigDecimal money(BigDecimal value) {
        return nvl(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
