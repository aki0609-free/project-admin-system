package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.project.backend.features.admin.business.repository.DormitoryFeeSettingRepository;
import com.project.backend.features.admin.business.entity.DormitoryFeeSetting;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.employee.enums.DormitoryType;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.master.payrollitem.service.PayrollItemDailyInputService;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalanceQueryService;
import com.project.backend.features.master.payrollitem.balance.PayrollItemBalanceSnapshot;
import com.project.backend.features.master.payrollitem.balance.BalanceUnit;
import com.project.backend.features.master.payrollitem.balance.EmployeePayrollItemSettingService;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;

class DailyReportInputItemServiceTest {

    private PayrollItemDailyInputService payrollItemDailyInputService;
    private EmployeeRepository employeeRepository;
    private EmployeeContractRepository employeeContractRepository;
    private DormitoryFeeSettingRepository dormitoryFeeSettingRepository;
    private PayrollItemBalanceQueryService balanceQueryService;
    private EmployeePayrollItemSettingService payrollItemSettingService;
    private DailyReportRepository dailyReportRepository;
    private DailyReportInputItemService service;

    @BeforeEach
    void setUp() {
        payrollItemDailyInputService = mock(PayrollItemDailyInputService.class);
        employeeRepository = mock(EmployeeRepository.class);
        employeeContractRepository = mock(EmployeeContractRepository.class);
        dormitoryFeeSettingRepository = mock(DormitoryFeeSettingRepository.class);
        balanceQueryService = mock(PayrollItemBalanceQueryService.class);
        payrollItemSettingService = mock(EmployeePayrollItemSettingService.class);
        dailyReportRepository = mock(DailyReportRepository.class);
        when(dailyReportRepository.findByEmployeeIdAndWorkDateAndDeletedAtIsNull(
                anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(balanceQueryService.findDeductionBalance(
                anyLong(), anyLong(), any(LocalDate.class), any()))
                .thenReturn(PayrollItemBalanceSnapshot.untracked());
        when(payrollItemSettingService.isDailyReportInputEnabled(
                anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn(true);
        service = new DailyReportInputItemService(
                payrollItemDailyInputService,
                employeeRepository,
                employeeContractRepository,
                dormitoryFeeSettingRepository,
                balanceQueryService,
                payrollItemSettingService,
                dailyReportRepository
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
                .containsEntry("targetDate", LocalDate.of(2026, 7, 27))
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

    @Test
    void calculate_shouldPassDormitoryMasterAmountAndChargeDaysToRule() {
        DailyReportSaveRequest request = mockRequest();
        when(request.dormitoryChargeDays()).thenReturn(3);

        Employee employee = new Employee();
        employee.setId(10L);
        employee.updateDormitory(true, DormitoryType.SHARED_ROOM);

        DormitoryFeeSetting setting = new DormitoryFeeSetting();
        setting.setDormitoryType(DormitoryType.SHARED_ROOM);
        setting.setDailyAmount(BigDecimal.valueOf(450));
        setting.setActiveFlag(true);

        when(employeeRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(employee));
        when(employeeContractRepository.findByEmployeeIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());
        when(dormitoryFeeSettingRepository
                .findByDormitoryTypeAndActiveFlagTrueAndDeletedAtIsNull(
                        DormitoryType.SHARED_ROOM
                ))
                .thenReturn(Optional.of(setting));
        when(payrollItemDailyInputService.findAllowanceItems(anyMap(), anyMap()))
                .thenReturn(List.of());
        when(payrollItemDailyInputService.findDeductionItems(anyMap(), anyMap()))
                .thenReturn(List.of());

        service.calculate(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> parametersCaptor =
                ArgumentCaptor.forClass(Map.class);
        verify(payrollItemDailyInputService).findDeductionItems(
                parametersCaptor.capture(),
                anyMap()
        );
        assertThat(parametersCaptor.getValue())
                .containsEntry("dormitoryFlag", true)
                .containsEntry("dormitoryType", "SHARED_ROOM")
                .containsEntry("dormitoryChargeDays", 3)
                .containsEntry("dormitoryDailyAmount", BigDecimal.valueOf(450));
    }

    @Test
    void calculate_shouldRejectQuantityExceedingRemainingBalance() {
        DailyReportSaveRequest request = mockRequest();
        when(request.deductions()).thenReturn(List.of(
                new DailyReportDeductionSaveRequest(
                        9L,
                        "MOBILE_RENTAL",
                        "携帯電話料",
                        40_000,
                        12_345,
                        true,
                        "会社都合による金額調整",
                        BigDecimal.valueOf(40),
                        BalanceUnit.DAYS.name()
                )
        ));

        Employee employee = new Employee();
        employee.setId(10L);
        when(employeeRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(employee));
        when(employeeContractRepository.findByEmployeeIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());
        when(payrollItemDailyInputService.findAllowanceItems(anyMap(), anyMap()))
                .thenReturn(List.of());
        when(payrollItemDailyInputService.findDeductionItems(anyMap(), anyMap()))
                .thenReturn(List.of(inputItem(9L, "MOBILE_RENTAL", 12_345)));
        when(balanceQueryService.findDeductionBalance(
                10L,
                9L,
                LocalDate.of(2026, 7, 27),
                null
        )).thenReturn(new PayrollItemBalanceSnapshot(
                true,
                BalanceUnit.DAYS,
                BigDecimal.ZERO,
                BigDecimal.valueOf(31),
                BigDecimal.ZERO,
                BigDecimal.valueOf(31)
        ));

        assertThatThrownBy(() -> service.calculate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("残数量を超えています")
                .hasMessageContaining("remaining=31")
                .hasMessageContaining("quantity=40");
    }

    @Test
    void calculate_shouldExcludeTransactionAndMonthlyOperationDeductions() {
        DailyReportSaveRequest request = mockRequest();
        Employee employee = new Employee();
        employee.setId(10L);

        when(employeeRepository.findByIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.of(employee));
        when(employeeContractRepository.findByEmployeeIdAndDeletedAtIsNull(10L))
                .thenReturn(Optional.empty());
        when(payrollItemDailyInputService.findAllowanceItems(anyMap(), anyMap()))
                .thenReturn(List.of());
        when(payrollItemDailyInputService.findDeductionItems(anyMap(), anyMap()))
                .thenReturn(List.of(
                        inputItem(2L, "DAILY_ITEM", 300),
                        inputItem(3L, "MOBILE_RENTAL", 5000),
                        inputItem(4L, "DORMITORY_FEE", 15000)
                ));
        when(payrollItemSettingService.isDailyReportInputEnabled(
                10L, 3L, LocalDate.of(2026, 7, 27)))
                .thenReturn(false);
        when(payrollItemSettingService.isDailyReportInputEnabled(
                10L, 4L, LocalDate.of(2026, 7, 27)))
                .thenReturn(false);

        var response = service.calculate(request);

        assertThat(response.deductions())
                .extracting(DailyReportInputItemResponse::code)
                .containsExactly("DAILY_ITEM");
    }

    private DailyReportSaveRequest mockRequest() {
        DailyReportSaveRequest request = mock(DailyReportSaveRequest.class);
        when(request.employeeId()).thenReturn(10L);
        when(request.workDate()).thenReturn(LocalDate.of(2026, 7, 27));
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
