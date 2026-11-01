package com.project.backend.features.system.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.backend.features.system.calendar.enums.CalendarRollDirection;
import com.project.backend.features.system.calendar.enums.CalendarType;

@ExtendWith(MockitoExtension.class)
class CalendarAdjustmentServiceTest {
    @Mock CalendarQueryService queryService;

    private final CalendarDayPolicy policy = new CalendarDayPolicy(
            Set.of(CalendarType.COMPANY), Set.of(DayOfWeek.SUNDAY)
    );

    @Test
    void rollsForwardUntilCalendarIsOpen() {
        LocalDate sunday = LocalDate.of(2026, 8, 16);
        LocalDate monday = sunday.plusDays(1);
        when(queryService.isClosed(sunday, policy)).thenReturn(true);
        when(queryService.isClosed(monday, policy)).thenReturn(false);

        LocalDate result = new CalendarAdjustmentService(queryService)
                .adjust(sunday, CalendarRollDirection.FORWARD, policy);

        assertThat(result).isEqualTo(monday);
    }

    @Test
    void findsPreviousOpenDayBeforeBusinessDate() {
        LocalDate monday = LocalDate.of(2026, 8, 17);
        LocalDate sunday = monday.minusDays(1);
        LocalDate saturday = monday.minusDays(2);
        when(queryService.isClosed(sunday, policy)).thenReturn(true);
        when(queryService.isClosed(saturday, policy)).thenReturn(false);

        LocalDate result = new CalendarAdjustmentService(queryService)
                .previousOpenDayBefore(monday, policy);

        assertThat(result).isEqualTo(saturday);
    }
}
