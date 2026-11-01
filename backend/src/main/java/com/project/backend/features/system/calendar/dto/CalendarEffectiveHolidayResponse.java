package com.project.backend.features.system.calendar.dto;

import java.time.LocalDate;
import java.util.Set;

import com.project.backend.features.system.calendar.enums.CalendarHolidaySource;

public record CalendarEffectiveHolidayResponse(
        LocalDate holidayDate,
        String holidayName,
        Set<CalendarHolidaySource> sources
) {}
