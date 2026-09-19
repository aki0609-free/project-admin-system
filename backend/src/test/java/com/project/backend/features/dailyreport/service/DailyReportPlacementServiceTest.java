package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.project.backend.features.dailyreport.entity.DailyReport;
import com.project.backend.features.employee.entity.Employee;
import com.project.backend.features.operation.preparation.entity.DailyPreparation;
import com.project.backend.features.operation.preparation.entity.DailyPreparationAssignment;
import com.project.backend.features.operation.preparation.repository.DailyPreparationAssignmentRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationRepository;

class DailyReportPlacementServiceTest {

    @Test
    void applyAuthoritativePlacement_shouldOverrideClientPlacement() {
        DailyPreparationRepository preparationRepository =
                mock(DailyPreparationRepository.class);
        DailyPreparationAssignmentRepository assignmentRepository =
                mock(DailyPreparationAssignmentRepository.class);
        DailyReportPlacementService service = new DailyReportPlacementService(
                preparationRepository, assignmentRepository);

        LocalDate workDate = LocalDate.of(2026, 9, 15);
        Employee employee = new Employee();
        employee.setId(10L);
        DailyReport report = new DailyReport();
        report.setEmployee(employee);
        report.setWorkDate(workDate);
        report.setCustomerId(999L);
        report.setCustomerSiteId(999L);

        DailyPreparation preparation = new DailyPreparation();
        preparation.setId(20L);
        DailyPreparationAssignment assignment = new DailyPreparationAssignment();
        assignment.setCustomerId(30L);
        assignment.setCustomerName("顧客A");
        assignment.setCustomerSiteId(40L);
        assignment.setSiteName("現場A");

        when(preparationRepository.findByTargetDateAndDeletedAtIsNull(workDate))
                .thenReturn(Optional.of(preparation));
        when(assignmentRepository
                .findByPreparationIdAndEmployeeIdAndDeletedAtIsNull(20L, 10L))
                .thenReturn(Optional.of(assignment));

        service.applyAuthoritativePlacement(report);

        assertThat(report.getCustomerId()).isEqualTo(30L);
        assertThat(report.getCustomerSiteId()).isEqualTo(40L);
        assertThat(report.getCustomerName()).isEqualTo("顧客A");
        assertThat(report.getSiteName()).isEqualTo("現場A");
    }
}
