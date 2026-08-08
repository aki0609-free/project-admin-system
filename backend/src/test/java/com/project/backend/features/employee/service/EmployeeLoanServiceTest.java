package com.project.backend.features.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.employee.dto.EmployeeLoanSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeLoan;
import com.project.backend.features.employee.enums.ApprovalStatus;
import com.project.backend.features.employee.mapper.EmployeeLoanMapper;
import com.project.backend.features.employee.repository.EmployeeLoanRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;

class EmployeeLoanServiceTest {

    private EmployeeLoanRepository repository;
    private EmployeeRepository employeeRepository;
    private EmployeeLoanService service;

    @BeforeEach
    void setUp() {
        repository = mock(EmployeeLoanRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        service = new EmployeeLoanService(
                repository,
                employeeRepository,
                new EmployeeLoanMapper()
        );
    }

    @Test
    void create_shouldInitializeBalanceFromPrincipalAndApprove() {
        Employee employee = employee();
        when(employeeRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(employee));
        when(repository.save(any(EmployeeLoan.class)))
                .thenAnswer(invocation -> {
                    EmployeeLoan loan = invocation.getArgument(0);
                    loan.setId(10L);
                    return loan;
                });

        var response = service.create(request("100000", true));

        assertThat(response.currentBalance()).isEqualByComparingTo("100000");
        assertThat(response.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void create_shouldRejectSecondActiveLoan() {
        when(employeeRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(employee()));
        when(repository.existsByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNull(1L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request("100000", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("既に");
    }

    @Test
    void update_shouldRejectPrincipalChangeAfterRepaymentStarted() {
        EmployeeLoan loan = new EmployeeLoan();
        loan.setId(10L);
        loan.setEmployee(employee());
        loan.setPrincipal(new BigDecimal("100000"));
        loan.setCurrentBalance(new BigDecimal("80000"));
        when(repository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.update(10L, request("120000", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("返済開始後");
    }

    private Employee employee() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("E001");
        employee.setEmployeeName("テスト従業員");
        employee.setActiveFlag(true);
        return employee;
    }

    private EmployeeLoanSaveRequest request(String principal, boolean active) {
        EmployeeLoanSaveRequest request = new EmployeeLoanSaveRequest();
        request.setEmployeeId(1L);
        request.setPrincipal(new BigDecimal(principal));
        request.setMonthlyRepayment(new BigDecimal("10000"));
        request.setActiveFlag(active);
        return request;
    }
}
