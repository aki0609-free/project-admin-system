package com.project.backend.features.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.features.employee.dto.EmployeeResignRequest;
import com.project.backend.features.employee.dto.EmployeeSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.entity.EmployeePayrollProfile;
import com.project.backend.features.employee.enums.EmploymentStatus;
import com.project.backend.features.employee.enums.EmploymentType;
import com.project.backend.features.employee.mapper.EmployeeMapper;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeePayrollProfileRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.employee.repository.EmployeeResignationChecklistRepository;

class EmployeeAdminServiceTest {

    private EmployeeRepository employeeRepository;
    private EmployeePayrollProfileRepository payrollRepository;
    private EmployeeContractRepository contractRepository;
    private EmployeeResignationChecklistRepository checklistRepository;
    private EmployeeDeletionPolicy deletionPolicy;
    private EmployeeMapper mapper;
    private com.project.backend.features.master.payrollitem.balance.EmployeePayrollItemSettingService payrollItemSettingService;
    private EmployeeAdminService service;
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-22T01:02:03Z"),
            ZoneOffset.UTC
    );

    @BeforeEach
    void setUp() {
        employeeRepository = mock(EmployeeRepository.class);
        payrollRepository = mock(EmployeePayrollProfileRepository.class);
        contractRepository = mock(EmployeeContractRepository.class);
        checklistRepository = mock(EmployeeResignationChecklistRepository.class);
        deletionPolicy = mock(EmployeeDeletionPolicy.class);
        mapper = mock(EmployeeMapper.class);
        payrollItemSettingService = mock(
                com.project.backend.features.master.payrollitem.balance.EmployeePayrollItemSettingService.class
        );
        service = new EmployeeAdminService(
                employeeRepository,
                payrollRepository,
                contractRepository,
                checklistRepository,
                mapper,
                deletionPolicy,
                payrollItemSettingService,
                clock
        );
    }

    @Test
    void update_shouldRejectEmployeeCodeChange() {
        Employee employee = employee(1L, "E001", EmploymentStatus.ACTIVE);
        when(employeeRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.update(1L, request("E002")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("社員コード");
    }

    @Test
    void resign_shouldUseDedicatedStateTransition() {
        Employee employee = employee(1L, "E001", EmploymentStatus.ACTIVE);
        employee.setHireDate(LocalDate.of(2026, 4, 1));
        when(employeeRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(employee));
        when(checklistRepository
                .findAllByActiveFlagTrueAndRequiredFlagTrueAndDeletedAtIsNullOrderByDisplayOrderAscIdAsc())
                .thenReturn(List.of());
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(payrollRepository.findByEmployeeIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());
        when(contractRepository.findByEmployeeIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        service.resign(
                1L,
                new EmployeeResignRequest(
                        LocalDate.of(2026, 7, 31),
                        List.of(),
                        null
                )
        );

        assertThat(employee.getEmploymentStatus()).isEqualTo(EmploymentStatus.RESIGNED);
        assertThat(employee.isActiveFlag()).isFalse();
    }

    @Test
    void delete_shouldCheckBusinessReferencesBeforeSoftDelete() {
        Employee employee = employee(1L, "E001", EmploymentStatus.ACTIVE);
        when(employeeRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(employee));

        service.delete(1L);

        verify(deletionPolicy).verifyDeletable(1L);
        assertThat(employee.getDeletedAt()).isEqualTo(Instant.now(clock));
    }

    @Test
    void findDetail_shouldSupplyDefaultPayrollAndContractForLegacyEmployee() {
        Employee employee = employee(1L, "E001", EmploymentStatus.ACTIVE);
        when(employeeRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(employee));
        when(payrollRepository.findByEmployeeIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());
        when(contractRepository.findByEmployeeIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());
        when(payrollItemSettingService.findAll(1L)).thenReturn(List.of());

        service.findDetail(1L);

        ArgumentCaptor<EmployeePayrollProfile> payrollCaptor =
                ArgumentCaptor.forClass(EmployeePayrollProfile.class);
        ArgumentCaptor<EmployeeContract> contractCaptor =
                ArgumentCaptor.forClass(EmployeeContract.class);
        verify(mapper).toDetailResponse(
                org.mockito.ArgumentMatchers.eq(employee),
                payrollCaptor.capture(),
                contractCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(List.of())
        );
        assertThat(payrollCaptor.getValue().getEmployee()).isSameAs(employee);
        assertThat(contractCaptor.getValue().getEmployee()).isSameAs(employee);
        assertThat(payrollCaptor.getValue().getTaxCategory()).isNotNull();
        assertThat(contractCaptor.getValue().getSalaryType()).isNotNull();
    }

    private Employee employee(Long id, String code, EmploymentStatus status) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setEmployeeCode(code);
        employee.setEmploymentStatus(status);
        employee.setActiveFlag(status != EmploymentStatus.RESIGNED);
        return employee;
    }

    private EmployeeSaveRequest request(String code) {
        return new EmployeeSaveRequest(
                code,
                "テスト従業員",
                null,
                null,
                null,
                LocalDate.of(2026, 4, 1),
                null,
                EmploymentType.FULL_TIME,
                EmploymentStatus.ACTIVE,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                java.util.List.of()
        );
    }
}
