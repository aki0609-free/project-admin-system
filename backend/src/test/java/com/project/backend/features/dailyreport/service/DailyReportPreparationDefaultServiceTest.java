package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.dailyreport.dto.DailyReportPreparationDefaultResponse;
import com.project.backend.features.operation.preparation.entity.DailyPreparation;
import com.project.backend.features.operation.preparation.entity.DailyPreparationAssignment;
import com.project.backend.features.operation.preparation.repository.DailyPreparationAssignmentRepository;
import com.project.backend.features.operation.preparation.repository.DailyPreparationRepository;

class DailyReportPreparationDefaultServiceTest {

    private DailyPreparationRepository preparationRepository;
    private DailyPreparationAssignmentRepository assignmentRepository;
    private DailyReportPreparationDefaultService service;

    @BeforeEach
    void setUp() {
        preparationRepository = mock(DailyPreparationRepository.class);
        assignmentRepository = mock(DailyPreparationAssignmentRepository.class);
        service = new DailyReportPreparationDefaultService(
                preparationRepository,
                assignmentRepository
        );
    }

    @Test
    void find_shouldReturnAssignmentSnapshotForSameDateAndEmployee() {
        LocalDate date = LocalDate.of(2026, 9, 6);
        DailyPreparation preparation = new DailyPreparation();
        preparation.setId(1L);
        DailyPreparationAssignment assignment = new DailyPreparationAssignment();
        assignment.setId(2L);
        assignment.setPreparationId(1L);
        assignment.setEmployeeId(3L);
        assignment.setCustomerId(4L);
        assignment.setCustomerSiteId(5L);
        assignment.setCustomerName("顧客A");
        assignment.setSiteName("現場A");
        assignment.setWorkDescription("資材搬入");
        when(preparationRepository.findByTargetDateAndDeletedAtIsNull(date))
                .thenReturn(Optional.of(preparation));
        when(assignmentRepository
                .findByPreparationIdAndEmployeeIdAndDeletedAtIsNull(1L, 3L))
                .thenReturn(Optional.of(assignment));

        DailyReportPreparationDefaultResponse result = service.find(date, 3L);

        assertThat(result.available()).isTrue();
        assertThat(result.customerId()).isEqualTo(4L);
        assertThat(result.customerSiteId()).isEqualTo(5L);
        assertThat(result.workDescription()).isEqualTo("資材搬入");
    }

    @Test
    void find_shouldReturnUnavailableWhenPreparationDoesNotExist() {
        LocalDate date = LocalDate.of(2026, 9, 6);
        when(preparationRepository.findByTargetDateAndDeletedAtIsNull(date))
                .thenReturn(Optional.empty());

        DailyReportPreparationDefaultResponse result = service.find(date, 3L);

        assertThat(result.available()).isFalse();
    }
}
