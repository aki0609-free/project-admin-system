package com.project.backend.features.admin.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class BusinessCalendarServiceTest {
    private final BusinessCalendarService service = new BusinessCalendarService();

    @Test
    void onlySundayIsWeeklyHoliday() {
        assertThat(service.isHoliday(LocalDate.of(2026, 8, 8))).isFalse();
        assertThat(service.isHoliday(LocalDate.of(2026, 8, 16))).isTrue();
        assertThat(service.isHoliday(LocalDate.of(2026, 8, 17))).isFalse();
    }

    @Test
    void augustThirteenthThroughFifteenthAreObonHolidays() {
        assertThat(service.isHoliday(LocalDate.of(2026, 8, 12))).isFalse();
        assertThat(service.isHoliday(LocalDate.of(2026, 8, 13))).isTrue();
        assertThat(service.isHoliday(LocalDate.of(2026, 8, 14))).isTrue();
        assertThat(service.isHoliday(LocalDate.of(2026, 8, 15))).isTrue();
        assertThat(service.isHoliday(LocalDate.of(2026, 8, 16))).isTrue();
    }

    @Test
    void monthlyPaymentTreatsSaturdayAndSundayAsHolidays() {
        assertThat(service.isMonthlyPaymentHoliday(LocalDate.of(2026, 8, 8))).isTrue();
        assertThat(service.isMonthlyPaymentHoliday(LocalDate.of(2026, 8, 9))).isTrue();
        assertThat(service.isMonthlyPaymentHoliday(LocalDate.of(2026, 8, 10))).isFalse();
    }

    @Test
    void monthlyPaymentRollsBackAcrossWeekendAndObon() {
        assertThat(service.previousMonthlyPaymentBusinessDay(LocalDate.of(2026, 8, 15)))
                .isEqualTo(LocalDate.of(2026, 8, 12));
        assertThat(service.previousMonthlyPaymentBusinessDay(LocalDate.of(2026, 11, 15)))
                .isEqualTo(LocalDate.of(2026, 11, 13));
    }
}
