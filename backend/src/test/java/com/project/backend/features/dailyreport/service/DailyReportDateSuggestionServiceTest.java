package com.project.backend.features.dailyreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.backend.features.employee.enums.PaymentCycle;
import com.project.backend.features.system.calendar.enums.CalendarRollDirection;
import com.project.backend.features.system.calendar.service.CalendarAdjustmentService;
import com.project.backend.features.system.calendar.service.CalendarDayPolicy;

@ExtendWith(MockitoExtension.class)
class DailyReportDateSuggestionServiceTest {
    @Mock CalendarAdjustmentService calendar;

    @Test
    void dailyPaymentUsesNextBusinessDay() {
        LocalDate workDate = LocalDate.of(2026, 8, 13);
        LocalDate paymentDate = LocalDate.of(2026, 8, 17);
        when(calendar.adjust(
                org.mockito.ArgumentMatchers.eq(workDate.plusDays(1)),
                org.mockito.ArgumentMatchers.eq(CalendarRollDirection.FORWARD),
                org.mockito.ArgumentMatchers.any(CalendarDayPolicy.class)
        )).thenReturn(paymentDate);
        var result = new DailyReportDateSuggestionService(calendar)
                .resolve(LocalDate.of(2026, 8, 14), workDate, PaymentCycle.DAILY);
        assertThat(result.paymentDate()).isEqualTo(paymentDate);
    }

    @Test
    void workDateDefaultsToPreviousBusinessDay() {
        LocalDate businessDate = LocalDate.of(2026, 8, 17);
        LocalDate previous = LocalDate.of(2026, 8, 12);
        when(calendar.previousOpenDayBefore(
                org.mockito.ArgumentMatchers.eq(businessDate),
                org.mockito.ArgumentMatchers.any(CalendarDayPolicy.class)
        )).thenReturn(previous);
        when(calendar.adjust(
                org.mockito.ArgumentMatchers.eq(previous.plusDays(1)),
                org.mockito.ArgumentMatchers.eq(CalendarRollDirection.FORWARD),
                org.mockito.ArgumentMatchers.any(CalendarDayPolicy.class)
        )).thenReturn(businessDate);
        var result = new DailyReportDateSuggestionService(calendar)
                .resolve(businessDate, null, PaymentCycle.DAILY);
        assertThat(result.workDate()).isEqualTo(previous);
    }

    @Test
    void weeklyPaymentStartsFromFollowingMondayAndRollsForward() {
        LocalDate workDate = LocalDate.of(2026, 8, 13);
        LocalDate followingMonday = LocalDate.of(2026, 8, 17);
        LocalDate paymentDate = LocalDate.of(2026, 8, 18);
        when(calendar.adjust(
                org.mockito.ArgumentMatchers.eq(followingMonday),
                org.mockito.ArgumentMatchers.eq(CalendarRollDirection.FORWARD),
                org.mockito.ArgumentMatchers.any(CalendarDayPolicy.class)
        )).thenReturn(paymentDate);

        var result = new DailyReportDateSuggestionService(calendar)
                .resolve(LocalDate.of(2026, 8, 14), workDate, PaymentCycle.WEEKLY);

        assertThat(result.paymentDate()).isEqualTo(paymentDate);
    }

    @Test
    void monthlyPaymentStartsFromFollowingMonthFifteenthAndRollsBack() {
        LocalDate workDate = LocalDate.of(2026, 7, 31);
        LocalDate fifteenth = LocalDate.of(2026, 8, 15);
        LocalDate paymentDate = LocalDate.of(2026, 8, 12);
        when(calendar.adjust(
                org.mockito.ArgumentMatchers.eq(fifteenth),
                org.mockito.ArgumentMatchers.eq(CalendarRollDirection.BACKWARD),
                org.mockito.ArgumentMatchers.any(CalendarDayPolicy.class)
        )).thenReturn(paymentDate);

        var result = new DailyReportDateSuggestionService(calendar)
                .resolve(LocalDate.of(2026, 8, 3), workDate, PaymentCycle.MONTHLY);

        assertThat(result.paymentDate()).isEqualTo(paymentDate);
    }
}
