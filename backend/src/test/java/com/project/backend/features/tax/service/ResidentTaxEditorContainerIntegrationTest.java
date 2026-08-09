package com.project.backend.features.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.tax.dto.*;
import com.project.backend.features.tax.repository.ResidentTaxMonthlyRepository;
import com.project.backend.testsupport.ContainerIntegrationTest;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ResidentTaxEditorContainerIntegrationTest extends ContainerIntegrationTest {

    @Autowired
    private ResidentTaxEditorService service;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ResidentTaxMonthlyRepository monthlyRepository;

    @Autowired
    private MonthlyClosingRepository closingRepository;

    @Test
    void 画面入力を下書き検証して月別住民税へ確定する() {
        Employee employee = employee("RT-EDITOR-001", "住民税検証社員");
        var draft = service.saveDraft(request(employee.getId(), 2026, 12_000, 11_000));

        assertThat(draft.status()).isEqualTo("VALIDATED");
        assertThat(draft.batchId()).isNotNull();
        assertThat(draft.employees()).filteredOn(row -> row.employeeId().equals(employee.getId()))
                .singleElement().satisfies(row -> {
                    assertThat(row.months()).hasSize(12);
                    assertThat(row.months()).filteredOn(month -> month.month() == 6)
                            .singleElement().extracting(ResidentTaxMonthEditorResponse::draftTaxAmount)
                            .isEqualTo(12_000);
                });

        var confirmed = service.confirm(
                draft.batchId(),
                new ResidentTaxConfirmRequest("自治体通知の画面入力", false));

        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
        assertThat(monthlyRepository
                .findByEmployeeIdAndFiscalYearOrderByMonthAsc(employee.getId(), 2026))
                .hasSize(12);
        assertThat(monthlyRepository
                .findByEmployeeIdAndFiscalYearAndMonth(employee.getId(), 2026, 6))
                .get().extracting(value -> value.getTaxAmount()).isEqualTo(12_000);
        assertThat(monthlyRepository
                .findByEmployeeIdAndFiscalYearAndMonth(employee.getId(), 2026, 7))
                .get().extracting(value -> value.getTaxAmount()).isEqualTo(11_000);
    }

    @Test
    void 締め済み月の変更は再締め確認なしでは確定しない() {
        Employee employee = employee("RT-EDITOR-002", "締め済み検証社員");
        MonthlyClosing closing = new MonthlyClosing();
        closing.setTargetMonth(LocalDate.of(2027, 5, 1));
        closing.setStatus(MonthlyClosingStatus.CLOSED);
        closingRepository.saveAndFlush(closing);

        var draft = service.saveDraft(request(employee.getId(), 2026, 1_000, 900));
        assertThat(draft.hasClosedMonthChanges()).isTrue();

        assertThatThrownBy(() -> service.confirm(
                draft.batchId(),
                new ResidentTaxConfirmRequest("締め済み月の訂正", false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("再締め");
    }

    private Employee employee(String code, String name) {
        Employee employee = new Employee();
        employee.setEmployeeCode(code);
        employee.setEmployeeName(name);
        return employeeRepository.saveAndFlush(employee);
    }

    private ResidentTaxDraftSaveRequest request(
            Long employeeId, Integer fiscalYear, int june, int julyAndAfter) {
        List<ResidentTaxMonthInput> months = List.of(
                new ResidentTaxMonthInput(6, june),
                new ResidentTaxMonthInput(7, julyAndAfter),
                new ResidentTaxMonthInput(8, julyAndAfter),
                new ResidentTaxMonthInput(9, julyAndAfter),
                new ResidentTaxMonthInput(10, julyAndAfter),
                new ResidentTaxMonthInput(11, julyAndAfter),
                new ResidentTaxMonthInput(12, julyAndAfter),
                new ResidentTaxMonthInput(1, julyAndAfter),
                new ResidentTaxMonthInput(2, julyAndAfter),
                new ResidentTaxMonthInput(3, julyAndAfter),
                new ResidentTaxMonthInput(4, julyAndAfter),
                new ResidentTaxMonthInput(5, julyAndAfter));
        return new ResidentTaxDraftSaveRequest(
                fiscalYear,
                List.of(new ResidentTaxEmployeeInput(employeeId, months)));
    }
}
