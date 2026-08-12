package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dailyreport.entity.DailyPayRuleSetting;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.enums.DailyPayComponentType;
import com.project.backend.features.dailyreport.repository.DailyPayRuleSettingRepository;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.system.rule.dto.RuleContextRequest;
import com.project.backend.features.system.rule.dto.RuleExecutionResult;
import com.project.backend.features.system.rule.service.RuleExecutionService;

class DailyPayComponentCalculationServiceTest {

    private final DailyPayRuleSettingRepository repository =
            mock(DailyPayRuleSettingRepository.class);
    private final RuleExecutionService ruleExecutionService =
            mock(RuleExecutionService.class);
    private final DailyReportRepository dailyReportRepository =
            mock(DailyReportRepository.class);
    private final DailyPayComponentCalculationService service =
            new DailyPayComponentCalculationService(
                    repository,
                    ruleExecutionService,
                    dailyReportRepository
            );

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-a");
        when(dailyReportRepository
                .findByEmployeeIdAndWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateAscIdAsc(
                        any(), any(), any()
                ))
                .thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void monthlyEmployee_shouldUseConfiguredRulesForFourComponents() {
        when(repository
                .findByTenantIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a"
                ))
                .thenReturn(List.of(
                        setting(DailyPayComponentType.NORMAL_PAY, "NORMAL"),
                        setting(DailyPayComponentType.OVERTIME_PAY, "OVERTIME"),
                        setting(DailyPayComponentType.NIGHT_PAY, "NIGHT"),
                        setting(DailyPayComponentType.HOLIDAY_PAY, "HOLIDAY")
                ));
        when(ruleExecutionService.execute(eq("NORMAL"), any(RuleContextRequest.class)))
                .thenReturn(result(new BigDecimal("10000")));
        when(ruleExecutionService.execute(eq("OVERTIME"), any(RuleContextRequest.class)))
                .thenReturn(result(new BigDecimal("1250")));
        when(ruleExecutionService.execute(eq("NIGHT"), any(RuleContextRequest.class)))
                .thenReturn(result(new BigDecimal("500")));
        when(ruleExecutionService.execute(eq("HOLIDAY"), any(RuleContextRequest.class)))
                .thenReturn(result(new BigDecimal("1350")));

        DailyReport report = report();
        EmployeeContract contract = new EmployeeContract();
        contract.setSalaryType(SalaryType.MONTHLY);

        var amounts = service.calculate(report, contract, 10L);

        assertThat(amounts.normalPayAmount())
                .isEqualByComparingTo("10000");
        assertThat(amounts.overtimePayAmount())
                .isEqualByComparingTo("1250");
        assertThat(amounts.nightPayAmount())
                .isEqualByComparingTo("500");
        assertThat(amounts.holidayPayAmount())
                .isEqualByComparingTo("1350");
        assertThat(amounts.total()).isEqualByComparingTo("13100");
        verify(ruleExecutionService)
                .execute(eq("NORMAL"), any(RuleContextRequest.class));
    }

    @Test
    void noRuleSetting_shouldKeepLegacyHourlyTotalButSeparateComponents() {
        when(repository
                .findByTenantIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a"
                ))
                .thenReturn(List.of());
        DailyReport report = report();
        EmployeeContract contract = new EmployeeContract();
        contract.setSalaryType(SalaryType.HOURLY);
        contract.setHourlyWage(new BigDecimal("1000"));

        var amounts = service.calculate(report, contract, 10L);

        assertThat(amounts.normalPayAmount())
                .isEqualByComparingTo("8000");
        assertThat(amounts.overtimePayAmount())
                .isEqualByComparingTo("2500");
        assertThat(amounts.nightPayAmount())
                .isEqualByComparingTo("250");
        assertThat(amounts.holidayPayAmount())
                .isEqualByComparingTo("0");
        assertThat(amounts.total()).isEqualByComparingTo("10750");
    }

    @Test
    void monthlyEmployeeWithoutRule_shouldAllocateMonthlySalaryToNormalPay() {
        when(repository
                .findByTenantIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a"
                ))
                .thenReturn(List.of());
        DailyReport report = report();
        EmployeeContract contract = new EmployeeContract();
        contract.setSalaryType(SalaryType.MONTHLY);
        contract.setMonthlySalary(new BigDecimal("300000"));
        contract.setStandardWorkingHours(new BigDecimal("40"));

        var amounts = service.calculate(report, contract, 10L);

        assertThat(amounts.normalPayAmount())
                .isEqualByComparingTo("13846");
        assertThat(amounts.overtimePayAmount())
                .isEqualByComparingTo("4327");
        assertThat(amounts.nightPayAmount())
                .isEqualByComparingTo("433");
        assertThat(amounts.holidayPayAmount())
                .isEqualByComparingTo("0");
    }

    @Test
    void weeklyHoursOverForty_shouldBecomeOvertimeFromMondayBasedWeek() {
        when(repository
                .findByTenantIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a"
                ))
                .thenReturn(List.of());
        when(dailyReportRepository
                .findByEmployeeIdAndWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateAscIdAsc(
                        any(), any(), any()
                ))
                .thenReturn(List.of(
                        previous(LocalDate.of(2026, 8, 3), "8", "0"),
                        previous(LocalDate.of(2026, 8, 4), "8", "0"),
                        previous(LocalDate.of(2026, 8, 5), "8", "0"),
                        previous(LocalDate.of(2026, 8, 6), "8", "0"),
                        previous(LocalDate.of(2026, 8, 7), "8", "0")
                ));

        DailyReport report = report();
        report.setWorkDate(LocalDate.of(2026, 8, 8));
        report.setWorkHours(new BigDecimal("8"));
        report.setOvertimeHours(BigDecimal.ZERO);
        report.setNightWorkHours(BigDecimal.ZERO);
        EmployeeContract contract = hourlyContract("1000");

        var amounts = service.calculate(report, contract, 10L);

        assertThat(amounts.normalPayAmount()).isZero();
        assertThat(amounts.overtimePayAmount())
                .isEqualByComparingTo("10000");
    }

    @Test
    void monthlyOvertimeOverSixty_shouldUseFiftyPercentPremium() {
        when(repository
                .findByTenantIdAndActiveFlagTrueAndDeletedAtIsNull(
                        "tenant-a"
                ))
                .thenReturn(List.of());
        when(dailyReportRepository
                .findByEmployeeIdAndWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateAscIdAsc(
                        any(), any(), any()
                ))
                .thenReturn(List.of(
                        previous(LocalDate.of(2026, 8, 1), "0", "10"),
                        previous(LocalDate.of(2026, 8, 2), "0", "10"),
                        previous(LocalDate.of(2026, 8, 3), "0", "10"),
                        previous(LocalDate.of(2026, 8, 4), "0", "10"),
                        previous(LocalDate.of(2026, 8, 5), "0", "10"),
                        previous(LocalDate.of(2026, 8, 6), "0", "10")
                ));

        DailyReport report = report();
        report.setWorkDate(LocalDate.of(2026, 8, 7));
        report.setWorkHours(BigDecimal.ZERO);
        report.setOvertimeHours(new BigDecimal("2"));
        report.setNightWorkHours(BigDecimal.ZERO);

        var amounts = service.calculate(report, hourlyContract("1000"), 10L);

        assertThat(amounts.overtimePayAmount())
                .isEqualByComparingTo("3000");
    }

    private DailyPayRuleSetting setting(
            DailyPayComponentType componentType,
            String ruleName
    ) {
        DailyPayRuleSetting setting = new DailyPayRuleSetting();
        setting.setComponentType(componentType);
        setting.setRuleName(ruleName);
        setting.setActiveFlag(true);
        return setting;
    }

    private RuleExecutionResult result(BigDecimal value) {
        return RuleExecutionResult.builder()
                .executed(true)
                .result(value)
                .build();
    }

    private DailyReport report() {
        DailyReport report = new DailyReport();
        report.setWorkHours(new BigDecimal("8"));
        report.setOvertimeHours(new BigDecimal("2"));
        report.setNightWorkHours(new BigDecimal("1"));
        report.setHolidayWorkHours(BigDecimal.ZERO);
        return report;
    }

    private DailyReport previous(
            LocalDate workDate,
            String workHours,
            String overtimeHours
    ) {
        DailyReport report = new DailyReport();
        report.setWorkDate(workDate);
        report.setWorkHours(new BigDecimal(workHours));
        report.setOvertimeHours(new BigDecimal(overtimeHours));
        return report;
    }

    private EmployeeContract hourlyContract(String hourlyWage) {
        EmployeeContract contract = new EmployeeContract();
        contract.setSalaryType(SalaryType.HOURLY);
        contract.setHourlyWage(new BigDecimal(hourlyWage));
        return contract;
    }
}
