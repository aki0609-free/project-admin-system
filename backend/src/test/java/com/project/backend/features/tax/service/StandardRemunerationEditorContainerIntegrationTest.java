package com.project.backend.features.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.repository.EmployeeRepository;
import com.project.backend.features.operation.monthly.entity.MonthlyClosing;
import com.project.backend.features.operation.monthly.enums.MonthlyClosingStatus;
import com.project.backend.features.operation.monthly.repository.MonthlyClosingRepository;
import com.project.backend.features.tax.dto.StandardRemunerationConfirmRequest;
import com.project.backend.features.tax.dto.StandardRemunerationDraftSaveRequest;
import com.project.backend.features.tax.dto.StandardRemunerationRowInput;
import com.project.backend.features.tax.entity.StandardSalary;
import com.project.backend.features.tax.repository.EmployeeStandardRemunerationRepository;
import com.project.backend.features.tax.repository.StandardSalaryRepository;
import com.project.backend.testsupport.ContainerIntegrationTest;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StandardRemunerationEditorContainerIntegrationTest extends ContainerIntegrationTest {
    private static final BigDecimal STANDARD_AMOUNT = new BigDecimal("220000");

    @Autowired StandardRemunerationEditorService service;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired EmployeeStandardRemunerationRepository remunerationRepository;
    @Autowired StandardSalaryRepository standardSalaryRepository;
    @Autowired MonthlyClosingRepository closingRepository;

    @BeforeEach
    void ensureStandardSalary() {
        if (!standardSalaryRepository.existsByStandardSalary(220_000)) {
            StandardSalary salary = new StandardSalary();
            salary.setMinSalary(210_000);
            salary.setMaxSalary(230_000);
            salary.setStandardSalary(220_000);
            standardSalaryRepository.save(salary);
        }
    }

    @Test
    void 下書き検証後に従業員別標準報酬月額を確定する() {
        Employee employee = employee("SR-EDITOR-001", "標準報酬検証社員");
        var draft = service.saveDraft(request(employee.getId(), LocalDate.of(2026, 4, 1), null));

        assertThat(draft.status()).isEqualTo("VALIDATED");
        service.confirm(draft.batchId(), new StandardRemunerationConfirmRequest("定時決定", false));

        assertThat(remunerationRepository.findAllByDeletedAtIsNullOrderByEmployeeIdAscEffectiveFromDesc())
                .filteredOn(row -> row.getEmployeeId().equals(employee.getId()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.getHealthStandardRemuneration()).isEqualByComparingTo(STANDARD_AMOUNT);
                    assertThat(row.getPensionStandardRemuneration()).isEqualByComparingTo(STANDARD_AMOUNT);
                });
    }

    @Test
    void 同一従業員の適用期間重複を拒否する() {
        Employee employee = employee("SR-EDITOR-002", "期間重複検証社員");
        var first = input(employee.getId(), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30));
        var second = input(employee.getId(), LocalDate.of(2026, 9, 1), null);

        assertThatThrownBy(() -> service.saveDraft(new StandardRemunerationDraftSaveRequest(List.of(first, second))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重複");
    }

    @Test
    void 標準報酬月額マスターにない金額を拒否する() {
        Employee employee = employee("SR-EDITOR-003", "マスター検証社員");
        var invalid = new StandardRemunerationRowInput(
                null, employee.getId(), LocalDate.of(2026, 4, 1), null,
                new BigDecimal("219999"), STANDARD_AMOUNT, "MANUAL", null);

        assertThatThrownBy(() -> service.saveDraft(new StandardRemunerationDraftSaveRequest(List.of(invalid))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("マスター");
    }

    @Test
    void 締め済み期間を含む変更は再締め確認なしでは確定しない() {
        Employee employee = employee("SR-EDITOR-004", "締め済み検証社員");
        MonthlyClosing closing = new MonthlyClosing();
        closing.setTargetMonth(LocalDate.of(2026, 9, 1));
        closing.setStatus(MonthlyClosingStatus.CLOSED);
        closingRepository.saveAndFlush(closing);

        var draft = service.saveDraft(request(employee.getId(), LocalDate.of(2026, 9, 1), null));
        assertThat(draft.hasClosedPeriodChanges()).isTrue();
        assertThatThrownBy(() -> service.confirm(
                draft.batchId(), new StandardRemunerationConfirmRequest("随時改定", false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("再締め");
        service.confirm(draft.batchId(), new StandardRemunerationConfirmRequest("随時改定", true));
    }

    private Employee employee(String code, String name) {
        Employee employee = new Employee();
        employee.setEmployeeCode(code);
        employee.setEmployeeName(name);
        return employeeRepository.saveAndFlush(employee);
    }

    private StandardRemunerationDraftSaveRequest request(Long employeeId, LocalDate from, LocalDate to) {
        return new StandardRemunerationDraftSaveRequest(List.of(input(employeeId, from, to)));
    }

    private StandardRemunerationRowInput input(Long employeeId, LocalDate from, LocalDate to) {
        return new StandardRemunerationRowInput(
                null, employeeId, from, to, STANDARD_AMOUNT, STANDARD_AMOUNT,
                "REGULAR_DECISION", "検証用");
    }
}
