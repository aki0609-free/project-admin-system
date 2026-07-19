package com.project.backend.features.employee.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.entity.EmployeeLoan;
import com.project.backend.features.employee.entity.EmployeeSaving;
import com.project.backend.features.employee.repository.EmployeeLoanRepository;
import com.project.backend.features.employee.repository.EmployeeSavingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeFinanceBalanceCommandService {

    private final EmployeeLoanRepository loanRepository;
    private final EmployeeSavingRepository savingRepository;

    public void applyDailyReportAmountDiff(
            Long employeeId,
            BigDecimal savingAmount,
            BigDecimal loanRepaymentAmount
    ) {
        applySavingAmount(employeeId, savingAmount);
        applyLoanRepaymentAmount(employeeId, loanRepaymentAmount);
    }

    private void applySavingAmount(
            Long employeeId,
            BigDecimal savingAmount
    ) {
        BigDecimal amount = nvl(savingAmount);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        EmployeeSaving saving = savingRepository
                .findFirstByEmployeeIdAndActiveFlagTrueOrderByIdDesc(employeeId)
                .orElse(null);

        if (saving == null) {
            return;
        }

        saving.setCurrentBalance(
                nvl(saving.getCurrentBalance()).add(amount)
        );

        savingRepository.save(saving);
    }

    private void applyLoanRepaymentAmount(
            Long employeeId,
            BigDecimal loanRepaymentAmount
    ) {
        BigDecimal amount = nvl(loanRepaymentAmount);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        EmployeeLoan loan = loanRepository
                .findFirstByEmployeeIdAndActiveFlagTrueOrderByIdDesc(employeeId)
                .orElse(null);

        if (loan == null) {
            return;
        }

        BigDecimal nextBalance =
                nvl(loan.getCurrentBalance()).subtract(amount);

        if (nextBalance.compareTo(BigDecimal.ZERO) < 0) {
            nextBalance = BigDecimal.ZERO;
        }

        loan.setCurrentBalance(nextBalance);

        loanRepository.save(loan);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}