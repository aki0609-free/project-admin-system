package com.project.backend.features.employee.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.backend.features.employee.dto.EmployeeFinanceSummaryResponse;
import com.project.backend.features.employee.entity.EmployeeLoan;
import com.project.backend.features.employee.entity.EmployeeSaving;
import com.project.backend.features.employee.repository.EmployeeLoanRepository;
import com.project.backend.features.employee.repository.EmployeeSavingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeFinanceQueryService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final EmployeeLoanRepository loanRepository;
    private final EmployeeSavingRepository savingRepository;

    public EmployeeFinanceSummaryResponse findSummary(Long employeeId) {
        EmployeeLoan loan = loanRepository
                .findFirstByEmployeeIdAndActiveFlagTrueOrderByIdDesc(employeeId)
                .orElse(null);

        EmployeeSaving saving = savingRepository
                .findFirstByEmployeeIdAndActiveFlagTrueOrderByIdDesc(employeeId)
                .orElse(null);

        return new EmployeeFinanceSummaryResponse(
                employeeId,
                loan == null ? BigDecimal.ZERO : nvl(loan.getCurrentBalance()),
                saving == null ? BigDecimal.ZERO : nvl(saving.getCurrentBalance()),
                loan == null ? BigDecimal.ZERO : nvl(loan.getMonthlyRepayment()),
                calculateMonthlySavingAmount(saving)
        );
    }

    private BigDecimal calculateMonthlySavingAmount(EmployeeSaving saving) {
        if (saving == null) {
            return BigDecimal.ZERO;
        }

        return nvl(saving.getMinSalaryThreshold())
                .multiply(nvl(saving.getPercentage()))
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}