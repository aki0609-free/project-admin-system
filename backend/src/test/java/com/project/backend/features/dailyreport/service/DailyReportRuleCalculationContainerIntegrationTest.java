package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.dto.DailyReportDeductionSaveRequest;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.repository.DailyReportDeductionRepository;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.dailyreport.enums.DailyReportInputMode;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionCalculationType;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;
import com.project.backend.features.master.deduction.enums.DeductionType;
import com.project.backend.features.master.deduction.enums.DeductionUnit;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;
import com.project.backend.features.master.payrollitem.balance.BalanceUnit;
import com.project.backend.features.master.payrollitem.service.PayrollItemDailyInputService;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.entity.RuleParameter;
import com.project.backend.features.system.rule.enums.RuleDataType;
import com.project.backend.features.system.rule.enums.RuleDslType;
import com.project.backend.features.system.rule.enums.RuleType;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;
import com.project.backend.testsupport.ContainerIntegrationTest;

@Transactional
class DailyReportRuleCalculationContainerIntegrationTest
        extends ContainerIntegrationTest {

    @Autowired
    private PayrollItemDailyInputService payrollItemDailyInputService;

    @Autowired
    private DailyReportDeductionCommandService deductionCommandService;

    @Autowired
    private RuleMasterRepository ruleMasterRepository;

    @Autowired
    private DeductionMasterRepository deductionMasterRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DailyReportRepository dailyReportRepository;

    @Autowired
    private DailyReportDeductionRepository deductionRepository;

    @Test
    void ruleCalculationAndManualOverrideArePersistedWithAuditDetails() {
        RuleMaster rule = saveDailyChargeRule();
        DeductionMaster master = saveAutomaticDeduction(rule.getRuleName());

        var calculatedItems = payrollItemDailyInputService.findDeductionItems(
                Map.of("chargeDays", "3"),
                Map.of(master.getId(), 1_200)
        );

        assertThat(calculatedItems).hasSize(1);
        var calculated = calculatedItems.getFirst();
        assertThat(calculated.code()).isEqualTo("INTEGRATION_DAILY_CHARGE");
        assertThat(calculated.inputMode())
                .isEqualTo(DailyReportInputMode.AUTO_WITH_OVERRIDE);
        // dailyAmountはRuleパラメータのデフォルト450、chargeDaysは文字列から整数へ変換。
        assertThat(calculated.calculatedAmount()).isEqualTo(1_350);
        assertThat(calculated.amount()).isEqualTo(1_200);
        assertThat(calculated.manualOverride()).isTrue();

        DailyReport report = saveDailyReport();
        deductionCommandService.replaceAll(
                report.getId(),
                List.of(new DailyReportDeductionSaveRequest(
                        master.getId(),
                        master.getDeductionCode(),
                        master.getDeductionName(),
                        calculated.calculatedAmount(),
                        calculated.amount(),
                        calculated.manualOverride(),
                        "本人申告により当日の徴収額を調整",
                        BigDecimal.valueOf(3),
                        BalanceUnit.DAYS.name()
                ))
        );
        deductionRepository.flush();

        var saved = deductionRepository
                .findByDailyReportIdOrderByIdAsc(report.getId())
                .getFirst();
        assertThat(saved.getCalculatedAmount()).isEqualTo(1_350);
        assertThat(saved.getAmount()).isEqualTo(1_200);
        assertThat(saved.isManualOverrideFlag()).isTrue();
        assertThat(saved.getOverrideReason())
                .isEqualTo("本人申告により当日の徴収額を調整");
        assertThat(saved.getQuantity()).isEqualByComparingTo("3");
        assertThat(saved.getBalanceUnit()).isEqualTo("DAYS");
        assertThat(saved.getTenantId()).isEqualTo(TEST_TENANT_ID);
    }

    private RuleMaster saveDailyChargeRule() {
        RuleMaster rule = new RuleMaster();
        rule.setRuleName("INTEGRATION_DAILY_CHARGE_RULE");
        rule.setRuleDisplayName("統合テスト日次徴収Rule");
        rule.setRuleType(RuleType.DEDUCTION);
        rule.setDslType(RuleDslType.JEXL);
        rule.setDslText("dailyAmount * chargeDays");
        rule.setResultFactKey("result");
        rule.setPriority(100);
        rule.setActiveFlag(true);

        RuleParameter dailyAmount = new RuleParameter();
        dailyAmount.setParamName("dailyAmount");
        dailyAmount.setDataType(RuleDataType.DECIMAL);
        dailyAmount.setRequiredFlag(true);
        dailyAmount.setDefaultValue("450");
        dailyAmount.setOrderNo(1);
        rule.addParameter(dailyAmount);

        RuleParameter chargeDays = new RuleParameter();
        chargeDays.setParamName("chargeDays");
        chargeDays.setDataType(RuleDataType.INTEGER);
        chargeDays.setRequiredFlag(true);
        chargeDays.setOrderNo(2);
        rule.addParameter(chargeDays);

        return ruleMasterRepository.saveAndFlush(rule);
    }

    private DeductionMaster saveAutomaticDeduction(String ruleName) {
        DeductionMaster master = new DeductionMaster();
        master.setDeductionCode("INTEGRATION_DAILY_CHARGE");
        master.setDeductionName("統合テスト日次徴収");
        master.setDeductionType(DeductionType.COMPANY);
        master.setCalculationType(DeductionCalculationType.AUTO);
        master.setRuleName(ruleName);
        master.setDefaultAmount(0);
        master.setAllowManualInput(true);
        master.setMinAmount(0);
        master.setMaxAmount(10_000);
        master.setDeductionUnit(DeductionUnit.BOTH);
        master.setDetailViewType(DeductionDetailViewType.NONE);
        master.setShowOnDailyStatement(true);
        master.setShowOnMonthlyStatement(true);
        master.setCarryToMonthlySettlement(true);
        master.setDisplayOrder(10);
        master.setEnabled(true);
        return deductionMasterRepository.saveAndFlush(master);
    }

    private DailyReport saveDailyReport() {
        Employee employee = new Employee();
        employee.setEmployeeCode("RULE_CALCULATION_EMPLOYEE");
        employee.setEmployeeName("Rule計算テスト従業員");
        employee = employeeRepository.saveAndFlush(employee);

        DailyReport report = new DailyReport();
        report.setEmployee(employee);
        report.setWorkDate(LocalDate.of(2026, 8, 9));
        return dailyReportRepository.saveAndFlush(report);
    }
}
