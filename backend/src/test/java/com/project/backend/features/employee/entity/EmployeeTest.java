package com.project.backend.features.employee.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.project.backend.features.employee.enums.EmploymentStatus;
import com.project.backend.features.employee.enums.DormitoryType;

class EmployeeTest {

    @Test
    void updateDormitory_shouldRequireTypeAndClearItWhenLeaving() {
        Employee employee = new Employee();

        assertThatThrownBy(() -> employee.updateDormitory(true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("寮タイプ");

        employee.updateDormitory(true, DormitoryType.SINGLE_ROOM);
        assertThat(employee.isDormitoryFlag()).isTrue();
        assertThat(employee.getDormitoryType()).isEqualTo(DormitoryType.SINGLE_ROOM);

        employee.updateDormitory(false, DormitoryType.SHARED_ROOM);
        assertThat(employee.isDormitoryFlag()).isFalse();
        assertThat(employee.getDormitoryType()).isNull();
    }

    @Test
    void resign_shouldKeepStatusDateAndActiveFlagConsistent() {
        Employee employee = new Employee();
        employee.setHireDate(LocalDate.of(2026, 4, 1));
        employee.initializeEmployment();

        employee.resign(LocalDate.of(2026, 7, 31));

        assertThat(employee.getEmploymentStatus()).isEqualTo(EmploymentStatus.RESIGNED);
        assertThat(employee.getResignDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        assertThat(employee.isActiveFlag()).isFalse();
    }

    @Test
    void resign_shouldRejectDateBeforeHireDate() {
        Employee employee = new Employee();
        employee.setHireDate(LocalDate.of(2026, 4, 1));

        assertThatThrownBy(() -> employee.resign(LocalDate.of(2026, 3, 31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("入社日以降");
    }

    @Test
    void changeEmploymentStatus_shouldRejectResigned() {
        Employee employee = new Employee();

        assertThatThrownBy(() -> employee.changeEmploymentStatus(
                EmploymentStatus.RESIGNED
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("退職処理");
    }

    @Test
    void cancelResignation_shouldRestoreActiveEmployee() {
        Employee employee = new Employee();
        employee.resign(LocalDate.of(2026, 7, 31));

        employee.cancelResignation(EmploymentStatus.ACTIVE);

        assertThat(employee.getEmploymentStatus()).isEqualTo(EmploymentStatus.ACTIVE);
        assertThat(employee.getResignDate()).isNull();
        assertThat(employee.isActiveFlag()).isTrue();
    }
}
