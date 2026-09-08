package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;
import com.project.backend.features.dailyreport.repository.DailyReportRepository;

class DailyReportSaveValidatorTest {

    private final DailyReportRepository repository =
            mock(DailyReportRepository.class);
    private final DailyReportSaveValidator validator =
            new DailyReportSaveValidator(
                    repository,
                    new DailyReportWorkTimeCalculator()
            );

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

    @Test
    void validateForCreate_shouldRejectClientWorkHoursDifferentFromServerCalculation() {
        DailyReportSaveRequest request = validRequest();
        when(request.startTime()).thenReturn(LocalTime.of(8, 0));
        when(request.endTime()).thenReturn(LocalTime.of(18, 0));
        when(request.breakMinutes()).thenReturn(60);
        when(request.workHours()).thenReturn(new BigDecimal("7.00"));
        when(request.overtimeHours()).thenReturn(new BigDecimal("1.00"));
        when(request.nightWorkHours()).thenReturn(BigDecimal.ZERO);
        when(request.holidayWorkHours()).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> validator.validateForCreate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("通常時間")
                .hasMessageContaining("一致しません");
    }

    private DailyReportSaveRequest validRequest() {
        DailyReportSaveRequest request = mock(DailyReportSaveRequest.class);
        when(request.employeeId()).thenReturn(10L);
        when(request.workDate()).thenReturn(LocalDate.of(2026, 8, 9));
        when(request.customerSiteId()).thenReturn(null);
        return request;
    }
}
