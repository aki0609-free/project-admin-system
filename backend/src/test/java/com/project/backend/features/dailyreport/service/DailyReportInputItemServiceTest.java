package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.features.dailyreport.dto.DailyReportAllowanceSaveRequest;
import com.project.backend.features.dailyreport.dto.DailyReportDeductionSaveRequest;
import com.project.backend.features.dailyreport.dto.DailyReportInputItemResponse;
import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.master.payrollitem.service.PayrollItemDailyInputService;

class DailyReportInputItemServiceTest {

    private PayrollItemDailyInputService payrollItemDailyInputService;
    private EmployeeRepository employeeRepository;
    private EmployeeContractRepository employeeContractRepository;
    private DailyReportInputItemService service;

    @BeforeEach
    void setUp() {
        payrollItemDailyInputService = mock(PayrollItemDailyInputService.class);
        employeeRepository = mock(EmployeeRepository.class);
        employeeContractRepository = mock(EmployeeContractRepository.class);
        service = new DailyReportInputItemService(
                payrollItemDailyInputService,
                employeeRepository,
                employeeContractRepository
        );
    }

    @Test
    void calculate_shouldPassDailyReportContextAndManualAmountsToRuleCalculation() {
        DailyReportSaveRequest request = mockRequest();
        Employee employee = new Employee();
        employee.setId(10L);

        EmployeeContract contract = new EmployeeContract();
        contract.setSalaryType(SalaryType.HOURLY);
        contract.setHourlyWage(BigDecimal.valueOf(1500));

        when(employeeRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(employee));
        when(employeeContractRepository.findByEmployeeIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(contract));
        when(payrollItemDailyInputService.findAllowanceItems(anyMap(), anyMap()))
                .thenReturn(List.of(inputItem(1L, "OVERTIME", 1200)));
        when(payrollItemDailyInputService.findDeductionItems(anyMap(), anyMap()))
                .thenReturn(List.of());

        var response = service.calculate(request);

        assertThat(response.allowances()).hasSize(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> parametersCaptor =
                ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, Integer>> amountsCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(payrollItemDailyInputService).findAllowanceItems(
                parametersCaptor.capture(),
                amountsCaptor.capture()
        );

        assertThat(parametersCaptor.getValue())
                .containsEntry("employeeId", 10L)
                .containsEntry("targetDate", LocalDate.of(2026, 7, 25))
                .containsEntry("workHours", BigDecimal.valueOf(8))
                .containsEntry("overtimeHours", BigDecimal.valueOf(2))
                .containsEntry("salaryType", SalaryType.HOURLY)
                .containsEntry("hourlyWage", BigDecimal.valueOf(1500));
        assertThat(amountsCaptor.getValue()).containsEntry(1L, 500);
    }

    @Test
    void preview_shouldRejectDuplicateMasterIds() {
        DailyReportSaveRequest request = mockRequest();
        when(request.allowances()).thenReturn(List.of(
                new DailyReportAllowanceSaveRequest(1L, "A", "手当A", 100),
                new DailyReportAllowanceSaveRequest(1L, "A", "手当A", 200)
        ));

        Employee employee = new Employee();
        employee.setId(10L);
        when(employeeRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(employee));
        when(employeeContractRepository.findByEmployeeIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重複");
    }

    private DailyReportSaveRequest mockRequest() {
        DailyReportSaveRequest request = mock(DailyReportSaveRequest.class);
        when(request.employeeId()).thenReturn(10L);
        when(request.workDate()).thenReturn(LocalDate.of(2026, 7, 25));
        when(request.workHours()).thenReturn(BigDecimal.valueOf(8));
        when(request.overtimeHours()).thenReturn(BigDecimal.valueOf(2));
        when(request.allowances()).thenReturn(List.of(
                new DailyReportAllowanceSaveRequest(1L, "CLIENT_CODE", "client name", 500)
        ));
        when(request.deductions()).thenReturn(List.of(
                new DailyReportDeductionSaveRequest(2L, "CLIENT_CODE", "client name", 300)
        ));
        return request;
    }

    private DailyReportInputItemResponse inputItem(
            Long masterId,
            String code,
            Integer amount
    ) {
        return DailyReportInputItemResponse.builder()
                .masterId(masterId)
                .code(code)
                .name(code)
                .amount(amount)
                .build();
    }
}
