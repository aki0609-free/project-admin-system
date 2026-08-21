package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.admin.business.entity.DormitoryFeeSetting;
import com.project.backend.features.admin.business.repository.DormitoryFeeSettingRepository;
import com.project.backend.features.dailyreport.dto.DailyReportDeductionSaveRequest;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.repository.DailyReportDeductionRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.ApprovalStatus;
import com.project.backend.features.employee.enums.DormitoryType;
import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.enums.DeductionCalculationType;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;
import com.project.backend.features.master.deduction.enums.DeductionType;
import com.project.backend.features.master.deduction.enums.DeductionUnit;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;
import com.project.backend.features.master.payrollitem.balance.BalanceUnit;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalancePolicy;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalancePolicyRepository;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalanceQueryService;
import com.project.backend.features.master.payrollitem.balance.PayrollItemEnrollmentService;
import com.project.backend.features.master.payrollitem.balance.PayrollItemParameterDefinition;
import com.project.backend.features.master.payrollitem.balance.PayrollItemParameterDefinitionRepository;
import com.project.backend.features.master.payrollitem.balance.PayrollItemParameterInputType;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.features.admin.business.service.DormitoryDailyAmountRuleParameterResolver;
import com.project.backend.features.system.rule.entity.RuleMaster;
import com.project.backend.features.system.rule.enums.RuleDslType;
import com.project.backend.features.system.rule.enums.RuleType;
import com.project.backend.features.system.rule.repository.RuleMasterRepository;
import com.project.backend.testsupport.ContainerIntegrationTest;

@Transactional
class DailyReportTrackedDeductionContainerIntegrationTest
        extends ContainerIntegrationTest {

    private static final LocalDate WORK_DATE = LocalDate.of(2026, 8, 10);

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeContractRepository contractRepository;

    @Autowired
    private DeductionMasterRepository deductionMasterRepository;

    @Autowired
    private PayrollItemBalancePolicyRepository policyRepository;

    @Autowired
    private PayrollItemParameterDefinitionRepository parameterDefinitionRepository;

    @Autowired
    private PayrollItemEnrollmentService enrollmentService;

    @Autowired
    private PayrollItemBalanceQueryService balanceQueryService;

    @Autowired
    private RuleMasterRepository ruleMasterRepository;

    @Autowired
    private DormitoryFeeSettingRepository dormitoryFeeSettingRepository;

    @Autowired
    private DailyReportCommandService dailyReportCommandService;

    @Autowired
    private DailyReportInputItemService dailyReportInputItemService;

    @Autowired
    private DailyReportDeductionRepository deductionRepository;

    @Test
    void trackedDeductionsCalculateByQuantityAndKeepManualOverrideReason() {
        testClock.setDate(LocalDate.of(2026, 8, 1));
        Employee employee = saveEmployeeAndContract();
        saveDormitoryRuleAndFee();

        DeductionMaster dormitory = saveDeduction(
                "DORMITORY_FEE", "寮費", DeductionCalculationType.AUTO,
                "TEST_DORMITORY_DAILY_FEE", 0, 110);
        DeductionMaster mobile = saveDeduction(
                "MOBILE_RENTAL", "携帯電話貸出料", DeductionCalculationType.FIXED,
                null, 200, 120);
        savePolicy(dormitory);
        savePolicy(mobile);
        enrollmentService.synchronize(
                employee.getId(), PayrollItemTargetType.DEDUCTION,
                dormitory.getDeductionCode(), true,
                java.util.Map.of("dormitoryType", "SHARED_ROOM"));
        enrollmentService.synchronize(
                employee.getId(), PayrollItemTargetType.DEDUCTION,
                mobile.getDeductionCode(), true);

        var created = dailyReportCommandService.create(request(
                employee.getId(), null, false));

        var createdItems = deductionRepository
                .findByDailyReportIdOrderByIdAsc(created.id());
        assertThat(createdItems)
                .extracting(item -> item.getDeductionCode())
                .containsExactly("DORMITORY_FEE", "MOBILE_RENTAL");
        assertThat(createdItems)
                .filteredOn(item -> "DORMITORY_FEE".equals(item.getDeductionCode()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getCalculatedAmount()).isEqualTo(1_350);
                    assertThat(item.getAmount()).isEqualTo(1_350);
                    assertThat(item.getQuantity()).isEqualByComparingTo("3");
                });
        assertThat(createdItems)
                .filteredOn(item -> "MOBILE_RENTAL".equals(item.getDeductionCode()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getCalculatedAmount()).isEqualTo(1_000);
                    assertThat(item.getAmount()).isEqualTo(1_000);
                    assertThat(item.getQuantity()).isEqualByComparingTo("5");
                    assertThat(item.isManualOverrideFlag()).isFalse();
                });
        assertThat(created.deductionAmount()).isEqualByComparingTo("2350");
        assertThat(created.estimatedNetPayAmount()).isEqualByComparingTo("13400");
        assertRemaining(employee, dormitory, 28);
        assertRemaining(employee, mobile, 26);

        var updated = dailyReportCommandService.update(
                created.id(), request(employee.getId(), 900, true));

        var updatedMobile = deductionRepository
                .findByDailyReportIdOrderByIdAsc(updated.id()).stream()
                .filter(item -> "MOBILE_RENTAL".equals(item.getDeductionCode()))
                .findFirst()
                .orElseThrow();
        assertThat(updatedMobile.getCalculatedAmount()).isEqualTo(1_000);
        assertThat(updatedMobile.getAmount()).isEqualTo(900);
        assertThat(updatedMobile.isManualOverrideFlag()).isTrue();
        assertThat(updatedMobile.getOverrideReason()).isEqualTo("会社支給端末の調整");
        assertThat(updated.deductionAmount()).isEqualByComparingTo("2250");
        assertThat(updated.estimatedNetPayAmount()).isEqualByComparingTo("13500");
        assertRemaining(employee, dormitory, 28);
        assertRemaining(employee, mobile, 26);
    }

    @Test
    void disabledDeductionIsExcludedFromNewDailyReportPreviewImmediately() {
        testClock.setDate(WORK_DATE);
        Employee employee = saveEmployeeAndContract();
        saveDormitoryRuleAndFee();

        DeductionMaster dormitory = saveDeduction(
                "DORMITORY_FEE", "寮費", DeductionCalculationType.AUTO,
                "TEST_DORMITORY_DAILY_FEE", 0, 110);
        DeductionMaster mobile = saveDeduction(
                "MOBILE_RENTAL", "携帯電話貸出料", DeductionCalculationType.FIXED,
                null, 200, 120);
        savePolicy(dormitory);
        savePolicy(mobile);
        enrollmentService.synchronize(
                employee.getId(), PayrollItemTargetType.DEDUCTION,
                dormitory.getDeductionCode(), true,
                java.util.Map.of("dormitoryType", "SHARED_ROOM"));
        enrollmentService.synchronize(
                employee.getId(), PayrollItemTargetType.DEDUCTION,
                mobile.getDeductionCode(), true);

        enrollmentService.synchronize(
                employee.getId(), PayrollItemTargetType.DEDUCTION,
                mobile.getDeductionCode(), false);

        var preview = dailyReportInputItemService.preview(
                request(employee.getId(), null, false));

        assertThat(preview.deductions())
                .extracting(item -> item.code())
                .containsExactly("DORMITORY_FEE");
    }

    private Employee saveEmployeeAndContract() {
        Employee employee = new Employee();
        employee.setEmployeeCode("TRACKED-DEDUCTION-001");
        employee.setEmployeeName("日数控除テスト社員");
        employee.updateDormitory(true, DormitoryType.SHARED_ROOM);
        employee = employeeRepository.saveAndFlush(employee);

        EmployeeContract contract = new EmployeeContract();
        contract.setEmployee(employee);
        contract.setSalaryType(SalaryType.HOURLY);
        contract.setPaymentCycle(PaymentCycle.MONTHLY);
        contract.setHourlyWage(new BigDecimal("1500"));
        contract.setStandardWorkingHours(new BigDecimal("40"));
        contractRepository.saveAndFlush(contract);
        return employee;
    }

    private void saveDormitoryRuleAndFee() {
        RuleMaster rule = new RuleMaster();
        rule.setRuleName("TEST_DORMITORY_DAILY_FEE");
        rule.setRuleDisplayName("統合テスト日次寮費");
        rule.setRuleType(RuleType.DEDUCTION);
        rule.setDslType(RuleDslType.JEXL);
        rule.setDslText(
                "dormitoryDailyAmount * itemQuantity");
        rule.setResultFactKey("result");
        rule.setActiveFlag(true);
        ruleMasterRepository.saveAndFlush(rule);

        DormitoryFeeSetting fee = new DormitoryFeeSetting();
        fee.setDormitoryType(DormitoryType.SHARED_ROOM);
        fee.setDailyAmount(new BigDecimal("450"));
        fee.setActiveFlag(true);
        dormitoryFeeSettingRepository.saveAndFlush(fee);
    }

    private DeductionMaster saveDeduction(
            String code,
            String name,
            DeductionCalculationType calculationType,
            String ruleName,
            int defaultAmount,
            int displayOrder) {
        DeductionMaster master = new DeductionMaster();
        master.setDeductionCode(code);
        master.setDeductionName(name);
        master.setDeductionType(DeductionType.COMPANY);
        master.setCalculationType(calculationType);
        master.setRuleName(ruleName);
        master.setDefaultAmount(defaultAmount);
        master.setAllowManualInput(true);
        master.setDeductionUnit(DeductionUnit.BOTH);
        master.setDetailViewType(DeductionDetailViewType.NONE);
        master.setShowOnDailyStatement(true);
        master.setShowOnMonthlyStatement(true);
        master.setCarryToMonthlySettlement(true);
        master.setDisplayOrder(displayOrder);
        master.setEnabled(true);
        return deductionMasterRepository.saveAndFlush(master);
    }

    private void savePolicy(DeductionMaster master) {
        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setTargetType(PayrollItemTargetType.DEDUCTION);
        policy.setTargetMasterId(master.getId());
        policy.setTargetCode(master.getDeductionCode());
        policy.setDisplayName(master.getDeductionName());
        policy.setBalanceUnit(BalanceUnit.DAYS);
        policy.setAccrualFrequency("MONTHLY");
        policy.setAccrualRuleName("CALENDAR_DAYS_IN_ENROLLMENT");
        policy.setCarryForwardFlag(true);
        policy.setAdvanceConsumptionFlag(false);
        policy.setActiveFlag(true);
        policy = policyRepository.saveAndFlush(policy);

        if ("DORMITORY_FEE".equals(master.getDeductionCode())) {
            PayrollItemParameterDefinition dormitoryType =
                    new PayrollItemParameterDefinition();
            dormitoryType.setBalancePolicyId(policy.getId());
            dormitoryType.setParameterKey("dormitoryType");
            dormitoryType.setDisplayName("寮タイプ");
            dormitoryType.setInputType(PayrollItemParameterInputType.SELECT);
            dormitoryType.setRequiredFlag(true);
            dormitoryType.setOptionsJson("[{\"label\":\"複数人部屋\",\"value\":\"SHARED_ROOM\"}]");
            dormitoryType.setRuleParameterFlag(false);
            dormitoryType.setActiveFlag(true);
            parameterDefinitionRepository.saveAndFlush(dormitoryType);

            PayrollItemParameterDefinition dailyAmount =
                    new PayrollItemParameterDefinition();
            dailyAmount.setBalancePolicyId(policy.getId());
            dailyAmount.setParameterKey("dormitoryDailyAmount");
            dailyAmount.setDisplayName("寮費日額");
            dailyAmount.setInputType(PayrollItemParameterInputType.NUMBER);
            dailyAmount.setRequiredFlag(true);
            dailyAmount.setDefaultValue("0");
            dailyAmount.setRuleParameterFlag(true);
            dailyAmount.setRuleValueResolverKey(
                    DormitoryDailyAmountRuleParameterResolver.KEY);
            dailyAmount.setActiveFlag(true);
            parameterDefinitionRepository.saveAndFlush(dailyAmount);
        }
    }

    private DailyReportSaveRequest request(
            Long employeeId,
            Integer mobileAmount,
            boolean mobileOverride) {
        return new DailyReportSaveRequest(
                employeeId,
                WORK_DATE,
                WORK_DATE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "日数管理控除の統合テスト",
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                60,
                new BigDecimal("8"),
                new BigDecimal("2"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                3,
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                ApprovalStatus.APPROVED,
                "統合テスト自動承認",
                List.of(),
                List.of(
                        deduction(
                                "DORMITORY_FEE", "寮費", 0,
                                false, null, 3),
                        deduction(
                                "MOBILE_RENTAL", "携帯電話貸出料",
                                mobileAmount == null ? 0 : mobileAmount,
                                mobileOverride,
                                mobileOverride ? "会社支給端末の調整" : null,
                                5)));
    }

    private DailyReportDeductionSaveRequest deduction(
            String code,
            String name,
            int amount,
            boolean override,
            String reason,
            int quantity) {
        Long masterId = deductionMasterRepository
                .findByTenantIdAndDeductionCodeAndDeletedAtIsNull(
                        TEST_TENANT_ID, code)
                .orElseThrow()
                .getId();
        return new DailyReportDeductionSaveRequest(
                masterId,
                code,
                name,
                amount,
                amount,
                override,
                reason,
                BigDecimal.valueOf(quantity),
                BalanceUnit.DAYS.name());
    }

    private void assertRemaining(
            Employee employee,
            DeductionMaster master,
            int expected) {
        assertThat(balanceQueryService.findDeductionBalance(
                employee.getId(), master.getId(), WORK_DATE, null)
                .remainingQuantity()).isEqualByComparingTo(String.valueOf(expected));
    }
}
