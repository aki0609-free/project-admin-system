package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.dailyreport.entity.DailyPayRuleSetting;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.enums.DailyPayComponentType;
import com.project.backend.features.dailyreport.repository.DailyPayRuleSettingRepository;
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
    private final DailyPayComponentCalculationService service =
            new DailyPayComponentCalculationService(
                    repository,
                    ruleExecutionService
            );

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-a");
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
                .isEqualByComparingTo("10000.00");
        assertThat(amounts.overtimePayAmount())
                .isEqualByComparingTo("1250.00");
        assertThat(amounts.nightPayAmount())
                .isEqualByComparingTo("500.00");
        assertThat(amounts.holidayPayAmount())
                .isEqualByComparingTo("1350.00");
        assertThat(amounts.total()).isEqualByComparingTo("13100.00");
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
                .isEqualByComparingTo("8000.00");
        assertThat(amounts.overtimePayAmount())
                .isEqualByComparingTo("2000.00");
        assertThat(amounts.nightPayAmount())
                .isEqualByComparingTo("1000.00");
        assertThat(amounts.holidayPayAmount())
                .isEqualByComparingTo("0.00");
        assertThat(amounts.total()).isEqualByComparingTo("11000.00");
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
                .isEqualByComparingTo("13846.15");
        assertThat(amounts.overtimePayAmount())
                .isEqualByComparingTo("0.00");
        assertThat(amounts.nightPayAmount())
                .isEqualByComparingTo("0.00");
        assertThat(amounts.holidayPayAmount())
                .isEqualByComparingTo("0.00");
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
}
