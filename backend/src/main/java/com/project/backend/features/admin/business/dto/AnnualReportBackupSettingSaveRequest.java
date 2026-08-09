package com.project.backend.features.admin.business.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AnnualReportBackupSettingSaveRequest(
        @NotNull @Min(1) @Max(12) Integer fiscalYearStartMonth,
        @NotNull @Min(0) @Max(90) Integer graceDays,
        @NotNull Boolean startupEnabled,
        @NotNull Boolean activeFlag
) {
}
