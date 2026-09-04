package com.project.backend.features.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.entity.EmployeeContract;

class EmployeeWorkEligibilityPolicyTest {

    private final EmployeeWorkEligibilityPolicy policy =
            new EmployeeWorkEligibilityPolicy();

    @Test
    void isEligible_shouldIncludeEmploymentAndContractBoundaryDates() {
        Employee employee = employee("2026-04-01", "2026-09-30");
        EmployeeContract contract = contract("2026-04-15", "2026-09-15");

        assertThat(policy.isEligible(employee, contract, date("2026-04-15"))).isTrue();
        assertThat(policy.isEligible(employee, contract, date("2026-09-15"))).isTrue();
    }

    @Test
    void isEligible_shouldRejectOutsideContractPeriod() {
        Employee employee = employee("2026-04-01", null);
        EmployeeContract contract = contract("2026-04-15", "2026-09-15");

        assertThat(policy.isEligible(employee, contract, date("2026-04-14"))).isFalse();
        assertThat(policy.isEligible(employee, contract, date("2026-09-16"))).isFalse();
        assertThatThrownBy(() -> policy.verifyEligible(
                employee, contract, date("2026-09-16")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("契約期間外");
    }

    @Test
    void isEligible_shouldUseEmploymentPeriodWhenContractIsMissing() {
        Employee employee = employee("2026-04-01", "2026-09-30");

        assertThat(policy.isEligible(employee, null, date("2026-03-31"))).isFalse();
        assertThat(policy.isEligible(employee, null, date("2026-04-01"))).isTrue();
        assertThat(policy.isEligible(employee, null, date("2026-09-30"))).isTrue();
        assertThat(policy.isEligible(employee, null, date("2026-10-01"))).isFalse();
    }

    @Test
    void overlaps_shouldIncludeEmployeeWhoseContractOverlapsClosingPeriod() {
        Employee employee = employee("2026-01-01", null);
        EmployeeContract contract = contract("2026-07-15", "2026-08-10");

        assertThat(policy.overlaps(
                employee, contract, date("2026-08-01"), date("2026-08-31")
        )).isTrue();
        assertThat(policy.overlaps(
                employee, contract, date("2026-09-01"), date("2026-09-30")
        )).isFalse();
    }

    private Employee employee(String hireDate, String resignDate) {
        Employee employee = new Employee();
        employee.setHireDate(dateOrNull(hireDate));
        employee.setResignDate(dateOrNull(resignDate));
        return employee;
    }

    private EmployeeContract contract(String startDate, String endDate) {
        EmployeeContract contract = new EmployeeContract();
        contract.setContractStartDate(dateOrNull(startDate));
        contract.setContractEndDate(dateOrNull(endDate));
        return contract;
    }

    private LocalDate date(String value) {
        return LocalDate.parse(value);
    }

    private LocalDate dateOrNull(String value) {
        return value == null ? null : date(value);
    }
}
