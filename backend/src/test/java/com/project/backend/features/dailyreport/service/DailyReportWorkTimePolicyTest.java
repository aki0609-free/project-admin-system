package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DailyReportWorkTimePolicyTest {

    @Test
    void eligibleHolidayWork_shouldMoveNormalAndOvertimeToHoliday() {
        DailyReportWorkTimePolicy.WorkTimes result =
                DailyReportWorkTimePolicy.resolve(
                        LocalDate.of(2026, 8, 8),
                        new BigDecimal("8"),
                        new BigDecimal("2"),
                        new BigDecimal("1"),
                        BigDecimal.ZERO,
                        true
                );

        assertThat(result.workHours()).isZero();
        assertThat(result.overtimeHours()).isZero();
        assertThat(result.nightWorkHours()).isEqualByComparingTo("1");
        assertThat(result.holidayWorkHours()).isEqualByComparingTo("10");
    }

    @Test
    void weekdayWork_shouldKeepEnteredCategories() {
        DailyReportWorkTimePolicy.WorkTimes result =
                DailyReportWorkTimePolicy.resolve(
                        LocalDate.of(2026, 8, 10),
                        new BigDecimal("8"),
                        new BigDecimal("2"),
                        new BigDecimal("1"),
                        BigDecimal.ZERO,
                        false
                );

        assertThat(result.workHours()).isEqualByComparingTo("8");
        assertThat(result.overtimeHours()).isEqualByComparingTo("2");
        assertThat(result.nightWorkHours()).isEqualByComparingTo("1");
        assertThat(result.holidayWorkHours()).isZero();
    }

    @Test
    void saturdayWork_shouldRemainRegularWhenHolidayPremiumIsNotSelected() {
        DailyReportWorkTimePolicy.WorkTimes result =
                DailyReportWorkTimePolicy.resolve(
                        LocalDate.of(2026, 8, 8),
                        new BigDecimal("8"),
                        new BigDecimal("2"),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        false
                );

        assertThat(result.workHours()).isEqualByComparingTo("8");
        assertThat(result.overtimeHours()).isEqualByComparingTo("2");
        assertThat(result.holidayWorkHours()).isZero();
    }
}
