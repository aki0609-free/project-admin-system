package com.project.backend.features.operation.daily.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.operation.daily.dto.DailyPaymentResponse;
import com.project.backend.features.operation.daily.mapper.DailyPaymentMapper;
import com.project.backend.features.operation.daily.repository.DailyPaymentRepository;

class DailyPaymentServiceTest {

    private final DailyPaymentRepository paymentRepository =
            mock(DailyPaymentRepository.class);
    private final DailyReportRepository reportRepository =
            mock(DailyReportRepository.class);
    private final EmployeeContractRepository contractRepository =
            mock(EmployeeContractRepository.class);
    private final DailyPaymentService service = new DailyPaymentService(
            paymentRepository,
            reportRepository,
            contractRepository,
            new DailyPaymentMapper(),
            Clock.fixed(
                    Instant.parse("2026-08-09T00:00:00Z"),
                    ZoneOffset.UTC
            )
    );

    @Test
    void findByPaymentDate_shouldAggregateSavedRuleResultsByEmployee() {
        LocalDate paymentDate = LocalDate.of(2026, 8, 10);
        when(paymentRepository
                .findByPaymentDateAndDeletedAtIsNullOrderByEmployeeCodeAscIdAsc(
                        paymentDate
                )).thenReturn(List.of());
        when(reportRepository
                .findByPaymentDateAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        paymentDate
                )).thenReturn(List.of(
                        report(10L, "E001", "富陽 太郎", "8000"),
                        report(10L, "E001", "富陽 太郎", "6500")
                ));
        when(contractRepository.findByEmployeeIdInAndDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of(contract(10L, PaymentCycle.DAILY)));

        List<DailyPaymentResponse> result = service.findByPaymentDate(paymentDate);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().employeeId()).isEqualTo(10L);
        assertThat(result.getFirst().plannedAmount())
                .isEqualByComparingTo("14500");
        assertThat(result.getFirst().actualAmount())
                .isEqualByComparingTo("14500");
    }

    @Test
    void findByPaymentDate_shouldNotGeneratePaymentForMonthlyCycle() {
        LocalDate paymentDate = LocalDate.of(2026, 8, 10);
        when(paymentRepository
                .findByPaymentDateAndDeletedAtIsNullOrderByEmployeeCodeAscIdAsc(
                        paymentDate
                )).thenReturn(List.of());
        when(reportRepository
                .findByPaymentDateAndDeletedAtIsNullOrderByWorkDateDescIdDesc(
                        paymentDate
                )).thenReturn(List.of(
                        report(10L, "E001", "富陽 太郎", "8000")
                ));
        when(contractRepository.findByEmployeeIdInAndDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of(contract(10L, PaymentCycle.MONTHLY)));

        assertThat(service.findByPaymentDate(paymentDate)).isEmpty();
    }

    private DailyReport report(
            Long employeeId,
            String employeeCode,
            String employeeName,
            String estimatedNetPayAmount
    ) {
        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setEmployeeCode(employeeCode);
        employee.setEmployeeName(employeeName);

        DailyReport report = new DailyReport();
        report.setEmployee(employee);
        report.setEstimatedNetPayAmount(new BigDecimal(estimatedNetPayAmount));
        return report;
    }

    private EmployeeContract contract(Long employeeId, PaymentCycle paymentCycle) {
        Employee employee = new Employee();
        employee.setId(employeeId);

        EmployeeContract contract = new EmployeeContract();
        contract.setEmployee(employee);
        contract.setPaymentCycle(paymentCycle);
        return contract;
    }
}
