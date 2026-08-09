package com.project.backend.features.master.payrollitem.balance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.entity.DailyReportDeduction;
import com.project.backend.features.dailyreport.repository.DailyReportDeductionRepository;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.testsupport.ContainerIntegrationTest;

@Transactional
class PayrollItemBalanceContainerIntegrationTest
        extends ContainerIntegrationTest {

    @Autowired
    private PayrollItemBalanceQueryService balanceQueryService;

    @Autowired
    private PayrollItemEnrollmentService enrollmentService;

    @Autowired
    private PayrollItemBalancePolicyRepository policyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DailyReportRepository dailyReportRepository;

    @Autowired
    private DailyReportDeductionRepository deductionRepository;

    @Test
    void dormitoryBalanceCarriesUnpaidDaysFromPartialStartMonth() {
        Employee employee = saveEmployee("BALANCE_DORMITORY");
        PayrollItemBalancePolicy policy = savePolicy(
                9_101L,
                "INTEGRATION_DORMITORY_BALANCE",
                "寮費"
        );

        testClock.setDate(LocalDate.of(2026, 7, 20));
        enrollmentService.synchronize(
                employee.getId(),
                PayrollItemTargetType.DEDUCTION,
                policy.getTargetCode(),
                true
        );

        saveConsumption(employee, policy, LocalDate.of(2026, 7, 21), 3);
        saveConsumption(employee, policy, LocalDate.of(2026, 7, 28), 4);
        saveConsumption(employee, policy, LocalDate.of(2026, 8, 7), 10);

        PayrollItemBalanceSnapshot balance = balanceQueryService
                .findDeductionBalance(
                        employee.getId(),
                        policy.getTargetMasterId(),
                        LocalDate.of(2026, 8, 10),
                        null
                );

        assertThat(balance.tracked()).isTrue();
        assertThat(balance.unit()).isEqualTo(BalanceUnit.DAYS);
        // 7/20〜7/31の12日分から、7日分をまとめて支払った残り。
        assertThat(balance.openingQuantity()).isEqualByComparingTo("5");
        assertThat(balance.accruedQuantity()).isEqualByComparingTo("31");
        assertThat(balance.consumedQuantity()).isEqualByComparingTo("10");
        assertThat(balance.remainingQuantity()).isEqualByComparingTo("26");
    }

    @Test
    void mobileBalanceDefensivelyClampsLegacyOverconsumptionAndExcludesEditedReport() {
        Employee employee = saveEmployee("BALANCE_MOBILE");
        PayrollItemBalancePolicy policy = savePolicy(
                9_102L,
                "INTEGRATION_MOBILE_BALANCE",
                "携帯電話料"
        );

        testClock.setDate(LocalDate.of(2026, 8, 1));
        enrollmentService.synchronize(
                employee.getId(),
                PayrollItemTargetType.DEDUCTION,
                policy.getTargetCode(),
                true
        );

        // 通常の保存経路は超過数量を拒否する。ここでは過去データや直接投入で
        // 超過値が存在しても、残日数を負数表示しない防御動作を確認する。
        DailyReport editedReport = saveConsumption(
                employee,
                policy,
                LocalDate.of(2026, 8, 5),
                40
        );
        DailyReportDeduction manualOverride = deductionRepository
                .findByDailyReportIdOrderByIdAsc(editedReport.getId())
                .getFirst();
        manualOverride.setAmount(12_345);
        manualOverride.setManualOverrideFlag(true);
        manualOverride.setOverrideReason("会社都合による金額調整");
        deductionRepository.saveAndFlush(manualOverride);

        PayrollItemBalanceSnapshot savedBalance = balanceQueryService
                .findDeductionBalance(
                        employee.getId(),
                        policy.getTargetMasterId(),
                        LocalDate.of(2026, 8, 5),
                        null
                );
        PayrollItemBalanceSnapshot editingPreview = balanceQueryService
                .findDeductionBalance(
                        employee.getId(),
                        policy.getTargetMasterId(),
                        LocalDate.of(2026, 8, 5),
                        editedReport.getId()
                );

        assertThat(savedBalance.accruedQuantity()).isEqualByComparingTo("31");
        assertThat(savedBalance.consumedQuantity()).isEqualByComparingTo("40");
        assertThat(savedBalance.remainingQuantity()).isEqualByComparingTo("0");
        assertThat(manualOverride.getAmount()).isEqualTo(12_345);
        assertThat(manualOverride.getCalculatedAmount()).isEqualTo(40_000);
        assertThat(manualOverride.isManualOverrideFlag()).isTrue();
        assertThat(manualOverride.getOverrideReason())
                .isEqualTo("会社都合による金額調整");

        // 編集中は保存済みの自明細を除外し、入力値の二重加算を防ぐ。
        assertThat(editingPreview.consumedQuantity())
                .isEqualByComparingTo("0");
        assertThat(editingPreview.remainingQuantity())
                .isEqualByComparingTo("31");
    }

    private Employee saveEmployee(String employeeCode) {
        Employee employee = new Employee();
        employee.setEmployeeCode(employeeCode);
        employee.setEmployeeName("残日数テスト従業員");
        return employeeRepository.saveAndFlush(employee);
    }

    private PayrollItemBalancePolicy savePolicy(
            Long masterId,
            String code,
            String displayName
    ) {
        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setTargetType(PayrollItemTargetType.DEDUCTION);
        policy.setTargetMasterId(masterId);
        policy.setTargetCode(code);
        policy.setDisplayName(displayName);
        policy.setBalanceUnit(BalanceUnit.DAYS);
        policy.setAccrualFrequency("MONTHLY");
        policy.setAccrualRuleName("CALENDAR_DAYS_IN_ENROLLMENT");
        policy.setCarryForwardFlag(true);
        policy.setAdvanceConsumptionFlag(false);
        policy.setActiveFlag(true);
        return policyRepository.saveAndFlush(policy);
    }

    private DailyReport saveConsumption(
            Employee employee,
            PayrollItemBalancePolicy policy,
            LocalDate workDate,
            int quantity
    ) {
        DailyReport report = new DailyReport();
        report.setEmployee(employee);
        report.setWorkDate(workDate);
        report = dailyReportRepository.saveAndFlush(report);

        DailyReportDeduction deduction = DailyReportDeduction.builder()
                .dailyReportId(report.getId())
                .deductionMasterId(policy.getTargetMasterId())
                .deductionCode(policy.getTargetCode())
                .deductionName(policy.getDisplayName())
                .amount(quantity * 1_000)
                .calculatedAmount(quantity * 1_000)
                .manualOverrideFlag(false)
                .quantity(BigDecimal.valueOf(quantity))
                .balanceUnit(BalanceUnit.DAYS.name())
                .build();
        deductionRepository.saveAndFlush(deduction);
        return report;
    }
}
