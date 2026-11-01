package com.project.backend.features.system.calendar.dto;

import java.time.LocalDate;

import com.project.backend.features.system.calendar.enums.CalendarType;
import com.project.backend.features.system.calendar.enums.CalendarDayType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CalendarHolidaySaveRequest(
        Long id,
        @NotNull CalendarType calendarType,
        @NotNull LocalDate holidayDate,
        @NotBlank String holidayName,
        String note,
        @NotNull Boolean activeFlag,
        @NotNull CalendarDayType dayType
) {}
