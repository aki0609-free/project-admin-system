package com.project.backend.features.admin.business.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MonthlyClosingOutputSaveRequest(
        @NotBlank @Size(max = 100) String reportCode,
        @NotNull @Min(1) Integer executionOrder,
        @NotNull Boolean activeFlag,
        @Min(1) @Max(100) Integer backupRetentionYears
) {
}
