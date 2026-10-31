package com.project.backend.features.admin.business.dto;

import java.time.LocalDate;

public record BusinessHolidayResponse(
        Long id,
        LocalDate holidayDate,
        String holidayName,
        String holidayType,
        String note,
        boolean activeFlag
) {}
