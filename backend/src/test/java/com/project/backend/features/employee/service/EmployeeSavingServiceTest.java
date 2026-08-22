package com.project.backend.features.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.employee.dto.EmployeeSavingSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeSaving;
import com.project.backend.features.employee.enums.ApprovalStatus;
import com.project.backend.features.employee.mapper.EmployeeSavingMapper;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.employee.repository.EmployeeSavingRepository;

class EmployeeSavingServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-22T00:00:00Z"),
            ZoneOffset.UTC
    );

    private EmployeeSavingRepository repository;
    private EmployeeRepository employeeRepository;
    private EmployeeSavingService service;

    @BeforeEach
    void setUp() {
        repository = mock(EmployeeSavingRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        service = new EmployeeSavingService(
                repository,
                employeeRepository,
                new EmployeeSavingMapper(),
                CLOCK
        );
    }

    @Test
    void create_shouldInitializeZeroBalanceAndApprove() {
        when(employeeRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(employee()));
        when(repository.save(any(EmployeeSaving.class)))
                .thenAnswer(invocation -> {
                    EmployeeSaving saving = invocation.getArgument(0);
                    saving.setId(20L);
                    return saving;
                });

        var response = service.create(request(true));

        assertThat(response.currentBalance()).isEqualByComparingTo("0");
        assertThat(response.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void create_shouldRejectSecondActiveSavingSetting() {
        when(employeeRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(employee()));
        when(repository.existsByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNull(1L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(request(true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("既に");
    }

    @Test
    void create_shouldRejectPercentageOverOneHundred() {
        when(employeeRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(employee()));
        EmployeeSavingSaveRequest request = request(true);
        request.setPercentage(new BigDecimal("100.01"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100%以下");
    }

    @Test
    void delete_shouldUseBusinessClockWhenBalanceIsZero() {
        EmployeeSaving saving = new EmployeeSaving();
        saving.setCurrentBalance(BigDecimal.ZERO);
        when(repository.findByIdAndDeletedAtIsNull(20L))
                .thenReturn(Optional.of(saving));

        service.delete(20L);

        assertThat(saving.getDeletedAt()).isEqualTo(CLOCK.instant());
    }

    private Employee employee() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("E001");
        employee.setEmployeeName("テスト従業員");
        employee.setActiveFlag(true);
        return employee;
    }

    private EmployeeSavingSaveRequest request(boolean active) {
        EmployeeSavingSaveRequest request = new EmployeeSavingSaveRequest();
        request.setEmployeeId(1L);
        request.setPercentage(new BigDecimal("10"));
        request.setMinSalaryThreshold(new BigDecimal("180000"));
        request.setActiveFlag(active);
        return request;
    }
}
