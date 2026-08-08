package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;

class DailyReportSaveValidatorTest {

    private final DailyReportRepository repository =
            mock(DailyReportRepository.class);
    private final DailyReportSaveValidator validator =
            new DailyReportSaveValidator(repository);

    @Test
    void validateForCreate_shouldRejectSameEmployeeAndWorkDate() {
        DailyReportSaveRequest request = validRequest();
        when(repository.existsByEmployeeIdAndWorkDateAndDeletedAtIsNull(
                10L,
                LocalDate.of(2026, 8, 9)
        )).thenReturn(true);

        assertThatThrownBy(() -> validator.validateForCreate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("同じ従業員・勤務日");
    }

    @Test
    void validateForUpdate_shouldExcludeCurrentReport() {
        DailyReportSaveRequest request = validRequest();
        when(repository.existsByEmployeeIdAndWorkDateAndIdNotAndDeletedAtIsNull(
                10L,
                LocalDate.of(2026, 8, 9),
                20L
        )).thenReturn(false);

        validator.validateForUpdate(20L, request);
    }

    private DailyReportSaveRequest validRequest() {
        DailyReportSaveRequest request = mock(DailyReportSaveRequest.class);
        when(request.employeeId()).thenReturn(10L);
        when(request.workDate()).thenReturn(LocalDate.of(2026, 8, 9));
        when(request.customerSiteId()).thenReturn(null);
        return request;
    }
}
