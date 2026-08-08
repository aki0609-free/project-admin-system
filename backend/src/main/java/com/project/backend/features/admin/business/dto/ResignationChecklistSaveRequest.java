package com.project.backend.features.admin.business.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResignationChecklistSaveRequest(
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description,
        @NotNull Boolean requiredFlag,
        @NotNull @Min(0) Integer displayOrder,
        @NotNull Boolean activeFlag
) {
}
