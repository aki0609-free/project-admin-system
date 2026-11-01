package com.project.backend.features.system.calendar.dto;

import java.time.DayOfWeek;
import java.util.Set;

import com.project.backend.features.system.calendar.enums.CalendarType;

import jakarta.validation.constraints.NotNull;

public record CalendarPolicySaveRequest(
        @NotNull CalendarType calendarType,
        @NotNull Set<DayOfWeek> closedWeekdays,
        @NotNull Set<CalendarType> includedCalendarTypes
) {}
