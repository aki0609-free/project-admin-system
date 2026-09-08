package com.project.backend.features.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.employee.entity.EmployeePayrollProfile;
import com.project.backend.features.employee.repository.EmployeePayrollProfileRepository;

class EmployeePaidLeaveBalanceCommandServiceTest {

    private final EmployeePayrollProfileRepository repository =
            mock(EmployeePayrollProfileRepository.class);
    private final EmployeePaidLeaveBalanceCommandService service =
            new EmployeePaidLeaveBalanceCommandService(repository);

    @Test
    void applyUsageDiff_shouldConsumeAndRestoreBalance() {
        EmployeePayrollProfile profile = new EmployeePayrollProfile();
        profile.setPaidLeaveRemainingDays(new BigDecimal("10.00"));
        when(repository.findForUpdateByEmployeeId(1L))
                .thenReturn(Optional.of(profile));

        service.applyUsageDiff(1L, new BigDecimal("1.50"));
        assertThat(profile.getPaidLeaveRemainingDays())
                .isEqualByComparingTo("8.50");

        service.applyUsageDiff(1L, new BigDecimal("-0.50"));
        assertThat(profile.getPaidLeaveRemainingDays())
                .isEqualByComparingTo("9.00");
    }

    @Test
    void applyUsageDiff_shouldRejectUsageBeyondBalance() {
        EmployeePayrollProfile profile = new EmployeePayrollProfile();
        profile.setPaidLeaveRemainingDays(BigDecimal.ONE);
        when(repository.findForUpdateByEmployeeId(1L))
                .thenReturn(Optional.of(profile));

        assertThatThrownBy(() ->
                service.applyUsageDiff(1L, new BigDecimal("1.50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("有給残日数を超えて");
    }
}
