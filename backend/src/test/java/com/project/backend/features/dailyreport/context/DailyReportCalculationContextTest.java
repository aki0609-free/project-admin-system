package com.project.backend.features.dailyreport.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.employee.enums.DormitoryType;

class DailyReportCalculationContextTest {

    @Test
    void toParameters_shouldExposeOnlyGenericEmployeeFactsForPayrollItemRules() {
        Employee employee = new Employee();
        employee.setId(10L);
        employee.updateDormitory(true, DormitoryType.SHARED_ROOM);

        var parameters = DailyReportCalculationContext.builder()
                .employee(employee)
                .build()
                .toParameters();

        assertThat(parameters)
                .containsEntry("employeeId", 10L)
                .doesNotContainKeys("dormitoryFlag", "dormitoryType");
    }
}
