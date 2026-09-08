package com.project.backend.features.dailyreport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.admin.business.service.PayrollPolicySettingService;
import com.project.backend.features.admin.business.service.PayrollPolicySettingService.PayrollPolicyValues;
import com.project.backend.features.dailyreport.dto.DailyPayComponentAmounts;
import com.project.backend.features.dailyreport.entity.DailyPayRuleSetting;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.enums.DailyPayComponentType;
import com.project.backend.features.dailyreport.repository.DailyPayRuleSettingRepository;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.master.payrollitem.service.PayrollMoneyPolicy;
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
    private final DailyReportRepository dailyReportRepository;
    private final PayrollMoneyPolicy moneyPolicy;
    private final PayrollPolicySettingService payrollPolicySettingService;

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
        PayrollPolicyValues policy = payrollPolicySettingService.resolve(
                report.getWorkDate()
        );
        PayTimeBreakdown payTimes = resolvePayTimes(
                report, employeeId, policy
        );
        Map<String, Object> parameters = parameters(
                report,
                contract,
                employeeId,
                payTimes,
                policy
        );
        Map<DailyPayComponentType, String> ruleNames = ruleNames();

        BigDecimal normal = calculateOne(
                DailyPayComponentType.NORMAL_PAY,
                ruleNames,
                parameters,
                policy
        );
        BigDecimal overtime = calculateOne(
                DailyPayComponentType.OVERTIME_PAY,
                ruleNames,
                parameters,
                policy
        );
        BigDecimal night = calculateOne(
                DailyPayComponentType.NIGHT_PAY,
                ruleNames,
                parameters,
                policy
        );
        BigDecimal holiday = calculateOne(
                DailyPayComponentType.HOLIDAY_PAY,
                ruleNames,
                parameters,
                policy
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
            PayrollPolicyValues policy
    ) {
        String ruleName = ruleNames.get(componentType);

        Map<String, Object> parameters = new LinkedHashMap<>(baseParameters);
        parameters.put("componentType", componentType.name());
        RuleExecutionResult result = ruleExecutionService.execute(
                ruleName,
                RuleContextRequest.builder()
                        .parameters(parameters)
                        .build()
        );
        BigDecimal amount = moneyPolicy.toDecimal(
                result.result(),
                "日報給与Rule計算結果"
        );
        if (amount.signum() < 0) {
            throw new IllegalStateException(
                    "日報給与Ruleの計算結果は0以上である必要があります。componentType="
                            + componentType
            );
        }
        return money(amount, policy);
    }

    private Map<DailyPayComponentType, String> ruleNames() {
        Map<DailyPayComponentType, String> result =
                new EnumMap<>(DailyPayComponentType.class);
        for (DailyPayRuleSetting setting : settingRepository
                .findByTenantIdAndActiveFlagTrueAndDeletedAtIsNull(
                        TenantContext.getTenantId()
                )) {
            if (setting.getComponentType() == null) {
                throw new IllegalStateException(
                        "日報給与Rule設定のcomponentTypeが未設定です。"
                );
            }
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

        for (DailyPayComponentType componentType : DailyPayComponentType.values()) {
            if (!StringUtils.hasText(result.get(componentType))) {
                throw new IllegalStateException(
                        "必須の日報給与Rule設定がありません。componentType="
                                + componentType
                );
            }
        }
        return result;
    }

    private Map<String, Object> parameters(
            DailyReport report,
            EmployeeContract contract,
            Long employeeId,
            PayTimeBreakdown payTimes,
            PayrollPolicyValues policy
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
        put(parameters, "holidayPremiumEligible",
                report.isHolidayPremiumEligible());
        put(parameters, "regularPayHours", payTimes.regularPayHours());
        put(parameters, "overtimeWithin60Hours",
                payTimes.overtimeWithin60Hours());
        put(parameters, "overtimeOver60Hours",
                payTimes.overtimeOver60Hours());
        put(parameters, "overtimeRate", policy.overtimeRate());
        put(parameters, "overtimeOver60Rate",
                policy.overtimeOverThresholdRate());
        put(parameters, "nightPremiumRate", policy.nightPremiumRate());
        put(parameters, "holidayRate", policy.statutoryHolidayRate());
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
                    calculationHourlyRate(contract, policy));
            put(parameters, "regularPayAmount",
                    calculationHourlyRate(contract, policy)
                            .multiply(payTimes.regularPayHours()));
        }
        return parameters;
    }

    /**
     * Ruleからも使用できる給与計算用の基礎時給。
     *
     * <p>MONTHLYは「月給 × 12 ÷（週所定労働時間 × 52）」で、
     * 年平均の月所定労働時間による換算と同じ結果になる。</p>
     */
    private BigDecimal calculationHourlyRate(
            EmployeeContract contract,
            PayrollPolicyValues policy
    ) {
        if (contract == null || contract.getSalaryType() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal standardWeeklyHours =
                nvl(contract.getStandardWorkingHours()).signum() > 0
                        ? contract.getStandardWorkingHours()
                        : policy.weeklyStatutoryHours();

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
            case DAILY -> nvl(contract.getDailyWage())
                    .divide(
                            policy.dailyStandardHours(),
                            8,
                            RoundingMode.HALF_UP
                    );
        };
    }

    private PayTimeBreakdown resolvePayTimes(
            DailyReport report,
            Long employeeId,
            PayrollPolicyValues policy
    ) {
        BigDecimal regularHours = nvl(report.getWorkHours());
        BigDecimal enteredOvertime = nvl(report.getOvertimeHours());
        if (employeeId == null || report.getWorkDate() == null) {
            return splitMonthlyThreshold(
                    regularHours,
                    enteredOvertime,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    policy
            );
        }

        LocalDate workDate = report.getWorkDate();
        LocalDate monthStart = workDate.withDayOfMonth(1);
        List<DailyReport> previousReports = workDate.equals(monthStart)
                ? List.of()
                : dailyReportRepository
                        .findByEmployeeIdAndWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateAscIdAsc(
                                employeeId,
                                monthStart,
                                workDate.minusDays(1)
                        );

        BigDecimal priorMonthlyOvertime = BigDecimal.ZERO;
        BigDecimal priorCurrentWeekHours = BigDecimal.ZERO;
        LocalDate currentWeekStart = weekStart(workDate, policy);
        Map<LocalDate, BigDecimal> weeklyHours = new LinkedHashMap<>();

        for (DailyReport previous : previousReports) {
            LocalDate previousWeekStart = weekStart(
                    previous.getWorkDate(), policy
            );
            BigDecimal weekHours = weeklyHours.getOrDefault(
                    previousWeekStart,
                    BigDecimal.ZERO
            );
            BigDecimal previousRegular = nvl(previous.getWorkHours());
            BigDecimal additionalWeeklyOvertime = additionalWeeklyOvertime(
                    weekHours,
                    previousRegular,
                    policy
            );
            priorMonthlyOvertime = priorMonthlyOvertime
                    .add(nvl(previous.getOvertimeHours()))
                    .add(additionalWeeklyOvertime);
            BigDecimal previousTotal = previousRegular
                    .add(nvl(previous.getOvertimeHours()));
            weeklyHours.put(
                    previousWeekStart,
                    weekHours.add(previousTotal)
            );
        }
        priorCurrentWeekHours = weeklyHours.getOrDefault(
                currentWeekStart,
                BigDecimal.ZERO
        );

        return splitMonthlyThreshold(
                regularHours,
                enteredOvertime,
                priorCurrentWeekHours,
                priorMonthlyOvertime,
                policy
        );
    }

    private PayTimeBreakdown splitMonthlyThreshold(
            BigDecimal regularHours,
            BigDecimal enteredOvertime,
            BigDecimal priorWeekHours,
            BigDecimal priorMonthlyOvertime,
            PayrollPolicyValues policy
    ) {
        BigDecimal weeklyOvertime = additionalWeeklyOvertime(
                priorWeekHours,
                regularHours,
                policy
        );
        BigDecimal statutoryOvertime = enteredOvertime.add(weeklyOvertime);
        BigDecimal remainingWithin60 = policy.monthlyOvertimeThresholdHours()
                .subtract(priorMonthlyOvertime)
                .max(BigDecimal.ZERO);
        BigDecimal within60 = statutoryOvertime.min(remainingWithin60);
        BigDecimal over60 = statutoryOvertime.subtract(within60);
        return new PayTimeBreakdown(
                regularHours.subtract(weeklyOvertime),
                within60,
                over60
        );
    }

    private BigDecimal additionalWeeklyOvertime(
            BigDecimal priorWeekHours,
            BigDecimal currentRegularHours,
            PayrollPolicyValues policy
    ) {
        BigDecimal remainingRegular = policy.weeklyStatutoryHours()
                .subtract(priorWeekHours)
                .max(BigDecimal.ZERO);
        return currentRegularHours
                .subtract(remainingRegular)
                .max(BigDecimal.ZERO)
                .min(currentRegularHours);
    }

    private LocalDate weekStart(
            LocalDate date,
            PayrollPolicyValues policy
    ) {
        return date.with(TemporalAdjusters.previousOrSame(
                policy.weekStartDay()
        ));
    }

    private BigDecimal money(
            BigDecimal value,
            PayrollPolicyValues policy
    ) {
        return nvl(value).setScale(0, policy.amountRoundingMode());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private record PayTimeBreakdown(
            BigDecimal regularPayHours,
            BigDecimal overtimeWithin60Hours,
            BigDecimal overtimeOver60Hours
    ) {
    }
}
