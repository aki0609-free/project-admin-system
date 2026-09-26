package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.project.backend.features.dailyreport.dto.DailyReportSaveRequest;

class DailyReportWorkTimeCalculatorTest {

    private final DailyReportWorkTimeCalculator calculator =
            new DailyReportWorkTimeCalculator();

    @Test
    void calculate_shouldSplitNormalOvertimeAndNightAcrossMidnight() {
        DailyReportSaveRequest request = request(
                LocalTime.of(20, 0),
                LocalTime.of(6, 0),
                60,
                false
        );

        DailyReportWorkTimePolicy.WorkTimes result = calculator.calculate(request);

        assertThat(result.workHours()).isEqualByComparingTo("8.00");
        assertThat(result.overtimeHours()).isEqualByComparingTo("1.00");
        assertThat(result.nightWorkHours()).isEqualByComparingTo("7.00");
        assertThat(result.holidayWorkHours()).isZero();
    }

    @Test
    void calculate_shouldMoveEffectiveHoursToHolidayBucket() {
        DailyReportSaveRequest request = request(
                LocalTime.of(8, 0),
                LocalTime.of(19, 0),
                60,
                true
        );

        DailyReportWorkTimePolicy.WorkTimes result = calculator.calculate(request);

        assertThat(result.workHours()).isZero();
        assertThat(result.overtimeHours()).isZero();
        assertThat(result.holidayWorkHours()).isEqualByComparingTo("10.00");
    }

    @Test
    void calculate_shouldRejectBreakLongerThanWorkInterval() {
        DailyReportSaveRequest request = request(
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                61,
                false
        );

        assertThatThrownBy(() -> calculator.calculate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("勤務区間を超えて");
    }

    @Test
    void resolveForSave_shouldPreferManualNormalOvertimeAndNightHours() {
        DailyReportSaveRequest request = request(
                LocalTime.of(20, 0),
                LocalTime.of(6, 0),
                60,
                false
        );
        when(request.workHours()).thenReturn(new BigDecimal("6.50"));
        when(request.overtimeHours()).thenReturn(new BigDecimal("1.50"));
        when(request.nightWorkHours()).thenReturn(new BigDecimal("6.00"));

        DailyReportWorkTimePolicy.WorkTimes result =
                calculator.resolveForSave(request);

        assertThat(result.workHours()).isEqualByComparingTo("6.50");
        assertThat(result.overtimeHours()).isEqualByComparingTo("1.50");
        assertThat(result.nightWorkHours()).isEqualByComparingTo("6.00");
        assertThat(result.holidayWorkHours()).isZero();
    }

    private DailyReportSaveRequest request(
            LocalTime start,
            LocalTime end,
            int breakMinutes,
            boolean holiday
    ) {
        DailyReportSaveRequest request = mock(DailyReportSaveRequest.class);
        when(request.startTime()).thenReturn(start);
        when(request.endTime()).thenReturn(end);
        when(request.breakMinutes()).thenReturn(breakMinutes);
        when(request.holidayPremiumEligible()).thenReturn(holiday);
        when(request.workHours()).thenReturn(BigDecimal.ZERO);
        when(request.overtimeHours()).thenReturn(BigDecimal.ZERO);
        when(request.nightWorkHours()).thenReturn(BigDecimal.ZERO);
        when(request.holidayWorkHours()).thenReturn(BigDecimal.ZERO);
        return request;
    }
}
