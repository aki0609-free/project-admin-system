package com.project.backend.features.admin.business.dto;

import com.project.backend.common.dayrule.dto.DayRule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record BusinessClosingSettingSaveRequest(
        @NotNull @Valid DayRule closingDay,
        @NotNull @Valid DayRule paymentDay
) {
}
