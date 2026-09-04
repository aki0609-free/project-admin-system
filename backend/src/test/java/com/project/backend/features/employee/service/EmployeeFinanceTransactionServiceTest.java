package com.project.backend.features.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeFinanceTransaction;
import com.project.backend.features.employee.enums.EmployeeFinanceAccountType;
import com.project.backend.features.employee.enums.EmployeeFinanceTransactionType;
import com.project.backend.features.employee.repository.EmployeeFinanceTransactionRepository;

class EmployeeFinanceTransactionServiceTest {

    private EmployeeFinanceTransactionRepository repository;
    private EmployeeFinanceTransactionService service;

    @BeforeEach
    void setUp() {
        repository = mock(EmployeeFinanceTransactionRepository.class);
        service = new EmployeeFinanceTransactionService(repository);
    }

    @Test
    void record_shouldSaveSignedAmountAndBeforeAfterBalances() {
        Employee employee = new Employee();
        employee.setId(1L);

        service.record(
                employee,
                EmployeeFinanceAccountType.LOAN,
                EmployeeFinanceTransactionType.LOAN_REPAYMENT,
                10L,
                100L,
                LocalDate.of(2026, 9, 1),
                new BigDecimal("100000"),
                new BigDecimal("80000"),
                "日報の貸付返済額反映"
        );

        ArgumentCaptor<EmployeeFinanceTransaction> captor =
                ArgumentCaptor.forClass(EmployeeFinanceTransaction.class);
        verify(repository).save(captor.capture());

        EmployeeFinanceTransaction saved = captor.getValue();
        assertThat(saved.getAmount()).isEqualByComparingTo("-20000");
        assertThat(saved.getBalanceBefore()).isEqualByComparingTo("100000");
        assertThat(saved.getBalanceAfter()).isEqualByComparingTo("80000");
        assertThat(saved.getDailyReportId()).isEqualTo(100L);
    }

    @Test
    void record_shouldSkipNoBalanceChange() {
        service.record(
                new Employee(),
                EmployeeFinanceAccountType.SAVING,
                EmployeeFinanceTransactionType.SAVING_DEPOSIT,
                20L,
                100L,
                LocalDate.of(2026, 9, 1),
                BigDecimal.TEN,
                BigDecimal.TEN,
                null
        );

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
