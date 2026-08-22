package com.project.backend.features.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.employee.entity.EmployeeLoan;
import com.project.backend.features.employee.entity.EmployeeSaving;
import com.project.backend.features.employee.enums.ApprovalStatus;
import com.project.backend.features.employee.repository.EmployeeLoanRepository;
import com.project.backend.features.employee.repository.EmployeeSavingRepository;

class EmployeeFinanceBalanceCommandServiceTest {

    private EmployeeLoanRepository loanRepository;
    private EmployeeSavingRepository savingRepository;
    private EmployeeFinanceBalanceCommandService service;

    @BeforeEach
    void setUp() {
        loanRepository = mock(EmployeeLoanRepository.class);
        savingRepository = mock(EmployeeSavingRepository.class);
        service = new EmployeeFinanceBalanceCommandService(
                loanRepository,
                savingRepository
        );
    }

    @Test
    void repayment_shouldImmediatelyReduceBalanceAndCompleteLoan() {
        EmployeeLoan loan = loan("100000");
        when(loanRepository.findFirstByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(1L))
                .thenReturn(Optional.of(loan));

        service.applyDailyReportAmountDiff(
                1L,
                BigDecimal.ZERO,
                new BigDecimal("100000")
        );

        assertThat(loan.getCurrentBalance()).isEqualByComparingTo("0");
        assertThat(loan.isActiveFlag()).isFalse();
    }

    @Test
    void repayment_shouldRejectAmountOverCurrentBalance() {
        EmployeeLoan loan = loan("30000");
        when(loanRepository.findFirstByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(1L))
                .thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.applyDailyReportAmountDiff(
                1L,
                BigDecimal.ZERO,
                new BigDecimal("30001")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("残高");
    }

    @Test
    void negativeDiff_shouldRestoreCompletedLoanForDailyReportCorrection() {
        EmployeeLoan loan = loan("0");
        loan.setActiveFlag(false);
        when(loanRepository
                .findFirstByEmployeeIdAndApprovalStatusAndDeletedAtIsNullOrderByIdDesc(
                        1L,
                        ApprovalStatus.APPROVED
                ))
                .thenReturn(Optional.of(loan));

        service.applyDailyReportAmountDiff(
                1L,
                BigDecimal.ZERO,
                new BigDecimal("-25000")
        );

        assertThat(loan.getCurrentBalance()).isEqualByComparingTo("25000");
        assertThat(loan.isActiveFlag()).isTrue();
    }

    @Test
    void negativeSavingDiff_shouldRestoreBalanceButNeverBelowZero() {
        EmployeeSaving saving = new EmployeeSaving();
        saving.setCurrentBalance(new BigDecimal("12000"));
        saving.setActiveFlag(true);
        when(savingRepository.findFirstByEmployeeIdAndActiveFlagTrueAndDeletedAtIsNullOrderByIdDesc(1L))
                .thenReturn(Optional.of(saving));

        service.applyDailyReportAmountDiff(
                1L,
                new BigDecimal("-2000"),
                BigDecimal.ZERO
        );
        assertThat(saving.getCurrentBalance()).isEqualByComparingTo("10000");

        assertThatThrownBy(() -> service.applyDailyReportAmountDiff(
                1L,
                new BigDecimal("-10001"),
                BigDecimal.ZERO
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("残高");
    }

    private EmployeeLoan loan(String balance) {
        EmployeeLoan loan = new EmployeeLoan();
        loan.setPrincipal(new BigDecimal("100000"));
        loan.setCurrentBalance(new BigDecimal(balance));
        loan.setActiveFlag(true);
        loan.setApprovalStatus(ApprovalStatus.APPROVED);
        return loan;
    }
}
