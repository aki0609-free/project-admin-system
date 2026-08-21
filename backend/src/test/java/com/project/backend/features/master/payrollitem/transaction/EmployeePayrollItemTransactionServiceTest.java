package com.project.backend.features.master.payrollitem.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.tenant.context.TenantContext;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.master.deduction.entity.DeductionMaster;
import com.project.backend.features.master.deduction.repository.DeductionMasterRepository;
import com.project.backend.features.master.allowance.repository.AllowanceMasterRepository;
import com.project.backend.features.master.payrollitem.balance.BalanceUnit;
import com.project.backend.features.master.payrollitem.balance.EmployeePayrollItemEnrollment;
import com.project.backend.features.master.payrollitem.balance.EmployeePayrollItemEnrollmentRepository;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalancePolicy;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalancePolicyRepository;
import com.project.backend.features.master.payrollitem.enums.PayrollItemInputSource;
import com.project.backend.features.master.payrollitem.enums.PayrollItemTargetType;

class EmployeePayrollItemTransactionServiceTest {

    private static final String TENANT_ID = "tenant-test";

    private EmployeePayrollItemTransactionRepository repository;
    private EmployeePayrollItemTransactionService service;

    @BeforeEach
    void setUp() {
        repository = mock(EmployeePayrollItemTransactionRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        DeductionMasterRepository deductionMasterRepository =
                mock(DeductionMasterRepository.class);
        AllowanceMasterRepository allowanceMasterRepository =
                mock(AllowanceMasterRepository.class);
        PayrollItemBalancePolicyRepository policyRepository =
                mock(PayrollItemBalancePolicyRepository.class);
        EmployeePayrollItemEnrollmentRepository enrollmentRepository =
                mock(EmployeePayrollItemEnrollmentRepository.class);

        TenantContext.setTenantId(TENANT_ID);

        Employee employee = new Employee();
        employee.setId(10L);
        employee.setTenantId(TENANT_ID);
        when(employeeRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(employee));

        PayrollItemBalancePolicy policy = new PayrollItemBalancePolicy();
        policy.setId(20L);
        policy.setTargetType(PayrollItemTargetType.DEDUCTION);
        policy.setTargetMasterId(30L);
        policy.setTargetCode("MOBILE_RENTAL");
        policy.setDisplayName("携帯電話料");
        policy.setBalanceUnit(BalanceUnit.AMOUNT);
        policy.setBalanceTrackingFlag(false);
        policy.setInputSource(PayrollItemInputSource.TRANSACTION);
        policy.setActiveFlag(true);
        when(policyRepository
                .findByTenantIdAndTargetTypeAndTargetCodeAndDeletedAtIsNull(
                        TENANT_ID,
                        PayrollItemTargetType.DEDUCTION,
                        "MOBILE_RENTAL"
                ))
                .thenReturn(Optional.of(policy));

        EmployeePayrollItemEnrollment enrollment =
                new EmployeePayrollItemEnrollment();
        enrollment.setEmployeeId(10L);
        enrollment.setBalancePolicyId(20L);
        enrollment.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        when(enrollmentRepository
                .findFirstByEmployeeIdAndBalancePolicyIdAndEffectiveToIsNullAndDeletedAtIsNullOrderByEffectiveFromDesc(
                        10L, 20L
                ))
                .thenReturn(Optional.of(enrollment));

        DeductionMaster master = DeductionMaster.builder()
                .id(30L)
                .deductionCode("MOBILE_RENTAL")
                .deductionName("携帯電話料")
                .enabled(true)
                .build();
        master.setTenantId(TENANT_ID);
        when(deductionMasterRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(30L, TENANT_ID))
                .thenReturn(Optional.of(master));
        when(repository.save(any(EmployeePayrollItemTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service = new EmployeePayrollItemTransactionService(
                repository,
                employeeRepository,
                deductionMasterRepository,
                allowanceMasterRepository,
                policyRepository,
                enrollmentRepository,
                new ObjectMapper(),
                Clock.fixed(
                        Instant.parse("2026-08-11T00:00:00Z"),
                        ZoneOffset.UTC
                ),
                mock(com.project.backend.features.master.payrollitem.balance.PayrollItemParameterDefinitionRepository.class)
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_shouldPersistConfirmedTransactionForTheTargetMonth() {
        var response = service.create(10L, request(LocalDate.of(2026, 8, 5)));

        ArgumentCaptor<EmployeePayrollItemTransaction> captor =
                ArgumentCaptor.forClass(EmployeePayrollItemTransaction.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTargetMonth())
                .isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(captor.getValue().getSourceType())
                .isEqualTo(PayrollItemTransactionSource.MANUAL);
        assertThat(response.amount()).isEqualByComparingTo("2500.00");
        assertThat(response.status())
                .isEqualTo(PayrollItemTransactionStatus.CONFIRMED);
    }

    @Test
    void create_shouldRejectTransactionDateOutsideTargetMonth() {
        assertThatThrownBy(() -> service.create(
                10L, request(LocalDate.of(2026, 9, 1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("対象月の範囲内");
    }

    @Test
    void create_shouldRejectBalanceEffectForUntrackedItem() {
        var request = new EmployeePayrollItemTransactionRequest(
                PayrollItemTargetType.DEDUCTION,
                "MOBILE_RENTAL",
                "2026-08",
                LocalDate.of(2026, 8, 5),
                BigDecimal.valueOf(2500),
                BigDecimal.ONE,
                PayrollItemTransactionStatus.CONFIRMED,
                "MOBILE-202608-2",
                null,
                PayrollItemBalanceEffect.CREDIT
        );

        assertThatThrownBy(() -> service.create(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("残高管理しない項目");
    }

    private EmployeePayrollItemTransactionRequest request(LocalDate date) {
        return new EmployeePayrollItemTransactionRequest(
                "MOBILE_RENTAL",
                "2026-08",
                date,
                BigDecimal.valueOf(2500),
                null,
                PayrollItemTransactionStatus.CONFIRMED,
                "MOBILE-202608-1",
                "8月1回目"
        );
    }
}
