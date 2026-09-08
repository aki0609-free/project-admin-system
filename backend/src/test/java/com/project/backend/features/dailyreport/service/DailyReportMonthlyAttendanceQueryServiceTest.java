package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.common.closing.service.ClosingSettingQueryService;
import com.project.backend.common.dayrule.dto.DayRule;
import com.project.backend.common.dayrule.enums.DayRuleType;
import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.entity.EmployeePayrollProfile;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeePayrollProfileRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.employee.service.EmployeeWorkEligibilityPolicy;

class DailyReportMonthlyAttendanceQueryServiceTest {

    private final DailyReportRepository dailyReportRepository =
            mock(DailyReportRepository.class);
    private final EmployeeRepository employeeRepository =
            mock(EmployeeRepository.class);
    private final EmployeeContractRepository contractRepository =
            mock(EmployeeContractRepository.class);
    private final EmployeePayrollProfileRepository payrollProfileRepository =
            mock(EmployeePayrollProfileRepository.class);
    private final ClosingSettingQueryService closingSettingQueryService =
            mock(ClosingSettingQueryService.class);
    private final EmployeeWorkEligibilityPolicy eligibilityPolicy =
            mock(EmployeeWorkEligibilityPolicy.class);

    private final DailyReportMonthlyAttendanceQueryService service =
            new DailyReportMonthlyAttendanceQueryService(
                    dailyReportRepository,
                    employeeRepository,
                    contractRepository,
                    payrollProfileRepository,
                    closingSettingQueryService,
                    eligibilityPolicy
            );

    @Test
    void findMonthlyAttendance_shouldUseSavedPayComponentsAndHolidayHours() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeCode("E001");
        employee.setEmployeeName("テスト社員");

        EmployeeContract contract = new EmployeeContract();
        contract.setEmployee(employee);
        contract.setSalaryType(SalaryType.DAILY);
        contract.setDailyWage(new BigDecimal("12000"));

        EmployeePayrollProfile profile = new EmployeePayrollProfile();
        profile.setEmployee(employee);
        profile.setPaidLeaveRemainingDays(new BigDecimal("8.00"));

        DailyReport report = new DailyReport();
        report.setEmployee(employee);
        report.setWorkDate(LocalDate.of(2026, 9, 5));
        report.setWorkHours(new BigDecimal("8.00"));
        report.setOvertimeHours(new BigDecimal("2.00"));
        report.setNightWorkHours(new BigDecimal("1.00"));
        report.setHolidayWorkHours(new BigDecimal("3.00"));
        report.setNormalPayAmount(new BigDecimal("12000"));
        report.setOvertimePayAmount(new BigDecimal("3750"));
        report.setNightPayAmount(new BigDecimal("750"));
        report.setHolidayPayAmount(new BigDecimal("5000"));
        report.setPaidLeaveDays(new BigDecimal("2.00"));

        when(closingSettingQueryService.getPayrollClosingDayRule())
                .thenReturn(DayRule.builder().type(DayRuleType.END_OF_MONTH).build());
        when(dailyReportRepository
                .findByWorkDateBetweenAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)
                )).thenReturn(List.of(report));
        when(employeeRepository.findAllByDeletedAtIsNullOrderByIdAsc())
                .thenReturn(List.of(employee));
        when(contractRepository.findByEmployeeIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(contract));
        when(payrollProfileRepository.findByEmployeeIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(profile));
        when(eligibilityPolicy.overlaps(any(), any(), any(), any()))
                .thenReturn(true);

        var response = service.findMonthlyAttendance(YearMonth.of(2026, 9));

        assertThat(response).singleElement().satisfies(item -> {
            assertThat(item.grossSalaryAmount()).isEqualByComparingTo("21500");
            assertThat(item.totalHolidayWorkHours()).isEqualByComparingTo("3.00");
            assertThat(item.paidLeaveRemainingDays()).isEqualByComparingTo("10.00");
            assertThat(item.paidLeaveRemainingAfterUsedDays())
                    .isEqualByComparingTo("8.00");
        });
    }
}
