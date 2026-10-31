package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.backend.features.admin.business.service.BusinessCalendarService;
import com.project.backend.features.employee.enums.PaymentCycle;

@ExtendWith(MockitoExtension.class)
class DailyReportDateSuggestionServiceTest {
    @Mock BusinessCalendarService calendar;

    @Test
    void dailyPaymentUsesNextBusinessDay() {
        LocalDate workDate = LocalDate.of(2026, 8, 13);
        LocalDate paymentDate = LocalDate.of(2026, 8, 17);
        when(calendar.nextBusinessDay(workDate.plusDays(1))).thenReturn(paymentDate);
        var result = new DailyReportDateSuggestionService(calendar)
                .resolve(LocalDate.of(2026, 8, 14), workDate, PaymentCycle.DAILY);
        assertThat(result.paymentDate()).isEqualTo(paymentDate);
    }

    @Test
    void workDateDefaultsToPreviousBusinessDay() {
        LocalDate businessDate = LocalDate.of(2026, 8, 17);
        LocalDate previous = LocalDate.of(2026, 8, 12);
        when(calendar.previousBusinessDay(businessDate)).thenReturn(previous);
        when(calendar.nextBusinessDay(previous.plusDays(1))).thenReturn(businessDate);
        var result = new DailyReportDateSuggestionService(calendar)
                .resolve(businessDate, null, PaymentCycle.DAILY);
        assertThat(result.workDate()).isEqualTo(previous);
    }

    @Test
    void weeklyPaymentStartsFromFollowingMondayAndRollsForward() {
        LocalDate workDate = LocalDate.of(2026, 8, 13);
        LocalDate followingMonday = LocalDate.of(2026, 8, 17);
        LocalDate paymentDate = LocalDate.of(2026, 8, 18);
        when(calendar.nextBusinessDay(followingMonday)).thenReturn(paymentDate);

        var result = new DailyReportDateSuggestionService(calendar)
                .resolve(LocalDate.of(2026, 8, 14), workDate, PaymentCycle.WEEKLY);

        assertThat(result.paymentDate()).isEqualTo(paymentDate);
    }

    @Test
    void monthlyPaymentStartsFromFollowingMonthFifteenthAndRollsBack() {
        LocalDate workDate = LocalDate.of(2026, 7, 31);
        LocalDate fifteenth = LocalDate.of(2026, 8, 15);
        LocalDate paymentDate = LocalDate.of(2026, 8, 12);
        when(calendar.previousMonthlyPaymentBusinessDay(fifteenth)).thenReturn(paymentDate);

        var result = new DailyReportDateSuggestionService(calendar)
                .resolve(LocalDate.of(2026, 8, 3), workDate, PaymentCycle.MONTHLY);

        assertThat(result.paymentDate()).isEqualTo(paymentDate);
    }
}
