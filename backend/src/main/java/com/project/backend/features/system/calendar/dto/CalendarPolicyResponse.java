package com.project.backend.features.system.calendar.dto;

import java.time.DayOfWeek;
import java.util.Set;
import java.util.List;

import com.project.backend.features.system.calendar.enums.CalendarType;

public record CalendarPolicyResponse(
        CalendarType calendarType,
        Set<DayOfWeek> closedWeekdays,
        Set<CalendarType> includedCalendarTypes,
        List<CalendarFixedPeriodResponse> fixedPeriods,
        boolean editable
) {}
