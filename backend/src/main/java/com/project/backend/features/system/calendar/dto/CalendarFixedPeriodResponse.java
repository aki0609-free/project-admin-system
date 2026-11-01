package com.project.backend.features.system.calendar.dto;

public record CalendarFixedPeriodResponse(
        String name,
        String startMonthDay,
        String endMonthDay
) {}
