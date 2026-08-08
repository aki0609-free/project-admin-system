package com.project.backend.features.employee.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.entity.EmployeeLoan;
import com.project.backend.features.employee.entity.EmployeeSaving;
import com.project.backend.features.employee.enums.ApprovalStatus;
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

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        EmployeeSaving saving = savingRepository
                .findFirstByEmployeeIdAndActiveFlagTrueOrderByIdDesc(employeeId)
                .orElse(null);

        if (saving == null) {
            throw new IllegalArgumentException(
                    "有効な積立設定がないため積立額を反映できません。employeeId=" + employeeId
            );
        }

        BigDecimal nextBalance = nvl(saving.getCurrentBalance()).add(amount);
        if (nextBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("積立残高を超えて取り消すことはできません。");
        }

        saving.setCurrentBalance(nextBalance);

        savingRepository.save(saving);
    }

    private void applyLoanRepaymentAmount(
            Long employeeId,
            BigDecimal loanRepaymentAmount
    ) {
        BigDecimal amount = nvl(loanRepaymentAmount);

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        EmployeeLoan loan = amount.compareTo(BigDecimal.ZERO) > 0
                ? loanRepository
                        .findFirstByEmployeeIdAndActiveFlagTrueOrderByIdDesc(employeeId)
                        .orElse(null)
                : loanRepository
                        .findFirstByEmployeeIdAndApprovalStatusAndDeletedAtIsNullOrderByIdDesc(
                                employeeId,
                                ApprovalStatus.APPROVED
                        )
                        .orElse(null);

        if (loan == null) {
            throw new IllegalArgumentException(
                    "返済対象の貸付がないため返済額を反映できません。employeeId=" + employeeId
            );
        }

        BigDecimal nextBalance =
                nvl(loan.getCurrentBalance()).subtract(amount);

        if (nextBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("返済額が貸付残高を超えています。");
        }
        if (nextBalance.compareTo(nvl(loan.getPrincipal())) > 0) {
            throw new IllegalArgumentException("返済取消後の残高が借入元本を超えています。");
        }

        loan.setCurrentBalance(nextBalance);
        loan.setActiveFlag(nextBalance.compareTo(BigDecimal.ZERO) > 0);

        loanRepository.save(loan);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
