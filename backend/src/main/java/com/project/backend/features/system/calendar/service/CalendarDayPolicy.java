package com.project.backend.features.system.calendar.service;

import java.time.DayOfWeek;
import java.util.Set;

import com.project.backend.features.system.calendar.enums.CalendarType;

public record CalendarDayPolicy(
        Set<CalendarType> calendarTypes,
        Set<DayOfWeek> closedWeekdays
) {
    public CalendarDayPolicy {
        calendarTypes = Set.copyOf(calendarTypes);
        closedWeekdays = Set.copyOf(closedWeekdays);
    }
}
