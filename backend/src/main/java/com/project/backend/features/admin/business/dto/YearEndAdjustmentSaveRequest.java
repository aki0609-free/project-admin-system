package com.project.backend.features.admin.business.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record YearEndAdjustmentSaveRequest(
        Long id,
        @NotNull Long employeeId,
        @NotNull @Min(2000) @Max(2200) Integer taxYear,
        @NotNull YearMonth targetMonth,
        @NotNull BigDecimal adjustmentAmount,
        @NotBlank String status,
        String note,
        Long lockVersion
) {}
