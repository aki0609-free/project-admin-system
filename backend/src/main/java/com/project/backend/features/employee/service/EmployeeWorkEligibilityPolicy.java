package com.project.backend.features.employee.service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;

/**
 * 従業員が指定日に勤務（日報登録）できるかを一元判定する。
 * 境界日は勤務可能として扱う。
 */
@Component
public class EmployeeWorkEligibilityPolicy {

    public void verifyEligible(
            Employee employee,
            EmployeeContract contract,
            LocalDate workDate
    ) {
        if (!isEligible(employee, contract, workDate)) {
            throw new IllegalArgumentException(
                    "指定した勤務日は従業員の在籍期間または契約期間外です。"
            );
        }
    }

    public boolean isEligible(
            Employee employee,
            EmployeeContract contract,
            LocalDate workDate
    ) {
        if (employee == null || workDate == null) {
            return false;
        }
        if (employee.getHireDate() != null
                && workDate.isBefore(employee.getHireDate())) {
            return false;
        }
        if (employee.getResignDate() != null
                && workDate.isAfter(employee.getResignDate())) {
            return false;
        }
        if (contract == null) {
            return true;
        }
        if (contract.getContractStartDate() != null
                && workDate.isBefore(contract.getContractStartDate())) {
            return false;
        }
        return contract.getContractEndDate() == null
                || !workDate.isAfter(contract.getContractEndDate());
    }

    public boolean overlaps(
            Employee employee,
            EmployeeContract contract,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        if (employee == null || periodStart == null || periodEnd == null) {
            return false;
        }
        LocalDate effectiveStart = latest(
                employee.getHireDate(),
                contract == null ? null : contract.getContractStartDate()
        );
        LocalDate effectiveEnd = earliest(
                employee.getResignDate(),
                contract == null ? null : contract.getContractEndDate()
        );
        return (effectiveStart == null || !effectiveStart.isAfter(periodEnd))
                && (effectiveEnd == null || !effectiveEnd.isBefore(periodStart));
    }

    private LocalDate latest(LocalDate left, LocalDate right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isAfter(right) ? left : right;
    }

    private LocalDate earliest(LocalDate left, LocalDate right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.isBefore(right) ? left : right;
    }
}
