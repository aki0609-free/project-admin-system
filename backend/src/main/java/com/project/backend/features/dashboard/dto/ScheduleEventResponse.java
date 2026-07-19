package com.project.backend.features.dashboard.dto;

import java.time.LocalDate;

public record ScheduleEventResponse(
    Long id,
    String title,
    LocalDate start,
    LocalDate end,
    String color,
    String description
) {}