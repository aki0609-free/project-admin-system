package com.project.backend.features.master.payrollitem.balance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;
import com.project.backend.testsupport.ContainerIntegrationTest;

@Transactional
class PayrollItemEnrollmentContainerIntegrationTest
        extends ContainerIntegrationTest {

    @Autowired
    private PayrollItemEnrollmentService enrollmentService;

    @Autowired
    private PayrollItemBalancePolicyRepository policyRepository;

    @Autowired
    private EmployeePayrollItemEnrollmentRepository enrollmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void enrollmentDatesFollowControllableBusinessClockAcrossMonths()
            throws Exception {
        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setTargetType(PayrollItemTargetType.DEDUCTION);
        policy.setTargetMasterId(9_001L);
        policy.setTargetCode("INTEGRATION_DORMITORY_FEE");
        policy.setDisplayName("統合テスト寮費");
        policy.setBalanceUnit(BalanceUnit.DAYS);
        policy.setAccrualFrequency("MONTHLY");
        policy.setAccrualRuleName("CALENDAR_DAYS_IN_ENROLLMENT");
        policy.setCarryForwardFlag(true);
        policy.setAdvanceConsumptionFlag(false);
        policy.setActiveFlag(true);
        policy = policyRepository.saveAndFlush(policy);

        testClock.setDate(LocalDate.of(2026, 1, 31));
        enrollmentService.synchronize(
                8_001L,
                PayrollItemTargetType.DEDUCTION,
                policy.getTargetCode(),
                true,
                Map.of("dormitoryType", "SINGLE")
        );
        enrollmentRepository.flush();

        EmployeePayrollItemEnrollment enrollment = enrollmentRepository
                .findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                        8_001L,
                        policy.getId()
                )
                .orElseThrow();

        assertThat(enrollment.getEffectiveFrom())
                .isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(objectMapper.readTree(enrollment.getSettingsJson())
                .path("dormitoryType").asText())
                .isEqualTo("SINGLE");

        testClock.setDate(LocalDate.of(2026, 2, 1));
        enrollmentService.synchronize(
                8_001L,
                PayrollItemTargetType.DEDUCTION,
                policy.getTargetCode(),
                false
        );
        enrollmentRepository.flush();

        assertThat(enrollment.getEffectiveTo())
                .isEqualTo(LocalDate.of(2026, 2, 1));
    }
}
