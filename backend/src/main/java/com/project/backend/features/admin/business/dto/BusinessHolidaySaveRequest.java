package com.project.backend.features.admin.business.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BusinessHolidaySaveRequest(
        Long id,
        @NotNull LocalDate holidayDate,
        @NotBlank String holidayName,
        @NotBlank String holidayType,
        String note,
        @NotNull Boolean activeFlag
) {}
