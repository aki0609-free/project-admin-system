package com.project.backend.features.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.employee.entity.EmployeeLoan;
import com.project.backend.features.employee.entity.EmployeeSaving;
import com.project.backend.features.employee.repository.EmployeeLoanRepository;
import com.project.backend.features.employee.repository.EmployeeSavingRepository;

class EmployeeFinanceQueryServiceTest {

    private EmployeeLoanRepository loanRepository;
    private EmployeeSavingRepository savingRepository;
    private EmployeeFinanceQueryService service;

    @BeforeEach
    void setUp() {
        loanRepository = mock(EmployeeLoanRepository.class);
        savingRepository = mock(EmployeeSavingRepository.class);
        service = new EmployeeFinanceQueryService(loanRepository, savingRepository);
    }

    @Test
    void findSummary_shouldReadOnlyActiveAndNonDeletedFinanceRecords() {
        EmployeeLoan loan = new EmployeeLoan();
        loan.setCurrentBalance(new BigDecimal("80000"));
        loan.setMonthlyRepayment(new BigDecimal("10000"));
        EmployeeSaving saving = new EmployeeSaving();
        saving.setCurrentBalance(new BigDecimal("15000"));
        saving.setMinSalaryThreshold(new BigDecimal("200000"));
        saving.setPercentage(new BigDecimal("5"));

        when(loanRepository
                .findFirstByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(1L))
                .thenReturn(Optional.of(loan));
        when(savingRepository
                .findFirstByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(1L))
                .thenReturn(Optional.of(saving));

        var summary = service.findSummary(1L);

        assertThat(summary.loanBalance()).isEqualByComparingTo("80000");
        assertThat(summary.savingBalance()).isEqualByComparingTo("15000");
        assertThat(summary.monthlyLoanRepayment()).isEqualByComparingTo("10000");
        assertThat(summary.monthlySavingAmount()).isEqualByComparingTo("10000.00");
        verify(loanRepository)
                .findFirstByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(1L);
        verify(savingRepository)
                .findFirstByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(1L);
    }
}
