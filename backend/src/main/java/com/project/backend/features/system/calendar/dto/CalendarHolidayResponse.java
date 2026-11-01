package com.project.backend.features.system.calendar.dto;

import java.time.LocalDate;

import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.enums.CalendarDayType;

public record CalendarHolidayResponse(
        Long id,
        CalendarType calendarType,
        LocalDate holidayDate,
        String holidayName,
        String note,
        boolean activeFlag,
        boolean editable,
        CalendarDayType dayType
) {}
