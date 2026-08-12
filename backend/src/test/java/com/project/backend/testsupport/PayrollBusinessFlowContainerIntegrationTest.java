package com.project.backend.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.service.DailyReportCommandService;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;
import com.project.backend.features.employee.enums.ApprovalStatus;
import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.employee.enums.SalaryType;
import com.project.backend.features.employee.repository.EmployeeContractRepository;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.operation.monthly.service.MonthlySummaryService;
import com.project.backend.features.tax.dto.ResidentTaxConfirmRequest;
import com.project.backend.features.tax.dto.ResidentTaxDraftSaveRequest;
import com.project.backend.features.tax.dto.ResidentTaxEmployeeInput;
import com.project.backend.features.tax.dto.ResidentTaxMonthInput;
import com.project.backend.features.tax.repository.ResidentTaxMonthlyRepository;
import com.project.backend.features.tax.service.ResidentTaxEditorService;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PayrollBusinessFlowContainerIntegrationTest
        extends ContainerIntegrationTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeContractRepository contractRepository;

    @Autowired
    private DailyReportCommandService dailyReportCommandService;

    @Autowired
    private ResidentTaxEditorService residentTaxEditorService;

    @Autowired
    private ResidentTaxMonthlyRepository residentTaxMonthlyRepository;

    @Autowired
    private MonthlySummaryService monthlySummaryService;

    @Test
    void employeeDailyPayResidentTaxAndMonthlySummaryRemainConsistent() {
        Employee employee = saveEmployeeAndContract();
        confirmResidentTax(employee.getId());

        var report = dailyReportCommandService.create(dailyReportRequest(employee.getId()));

        assertThat(report.normalPayAmount()).isEqualByComparingTo("12000");
        assertThat(report.overtimePayAmount()).isEqualByComparingTo("3750");
        assertThat(report.estimatedGrossPayAmount()).isEqualByComparingTo("15750");
        assertThat(report.estimatedNetPayAmount()).isEqualByComparingTo("15750");
        assertThat(report.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);

        assertThat(residentTaxMonthlyRepository
                .findByEmployeeIdAndFiscalYearAndMonth(employee.getId(), 2026, 8))
                .get()
                .extracting(value -> value.getTaxAmount())
                .isEqualTo(11_000);

        var summary = monthlySummaryService.findSummary("2026-08");
        assertThat(summary.employeeCount()).isEqualTo(1);
        assertThat(summary.workReportCount()).isEqualTo(1);
        assertThat(summary.totalGrossAmount()).isEqualByComparingTo("15750");
        assertThat(summary.totalDeductionAmount()).isEqualByComparingTo("0");
        assertThat(summary.totalDailyPaymentAmount()).isEqualByComparingTo("0");
        assertThat(summary.totalNetPaymentAmount()).isEqualByComparingTo("15750");
    }

    private Employee saveEmployeeAndContract() {
        Employee employee = new Employee();
        employee.setEmployeeCode("FLOW-EMP-001");
        employee.setEmployeeName("給与業務フローテスト社員");
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

    private void confirmResidentTax(Long employeeId) {
        List<ResidentTaxMonthInput> months = List.of(
                new ResidentTaxMonthInput(6, 12_000),
                new ResidentTaxMonthInput(7, 11_000),
                new ResidentTaxMonthInput(8, 11_000),
                new ResidentTaxMonthInput(9, 11_000),
                new ResidentTaxMonthInput(10, 11_000),
                new ResidentTaxMonthInput(11, 11_000),
                new ResidentTaxMonthInput(12, 11_000),
                new ResidentTaxMonthInput(1, 11_000),
                new ResidentTaxMonthInput(2, 11_000),
                new ResidentTaxMonthInput(3, 11_000),
                new ResidentTaxMonthInput(4, 11_000),
                new ResidentTaxMonthInput(5, 11_000));
        var draft = residentTaxEditorService.saveDraft(
                new ResidentTaxDraftSaveRequest(
                        2026,
                        List.of(new ResidentTaxEmployeeInput(employeeId, months))));
        residentTaxEditorService.confirm(
                draft.batchId(),
                new ResidentTaxConfirmRequest("業務フロー統合テスト", false));
    }

    private DailyReportSaveRequest dailyReportRequest(Long employeeId) {
        return new DailyReportSaveRequest(
                employeeId,
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 10),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Testcontainers固定日報",
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
                0,
                false,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                ApprovalStatus.APPROVED,
                "Testcontainers自動承認",
                List.of(),
                List.of());
    }
}
