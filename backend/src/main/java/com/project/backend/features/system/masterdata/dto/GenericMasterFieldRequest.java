package com.project.backend.features.system.masterdata.dto;

import com.project.backend.features.system.masterdata.entity.GenericMasterFieldType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GenericMasterFieldRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$")
        @Size(max = 100)
        String fieldKey,
        @NotBlank @Size(max = 200) String fieldLabel,
        @NotNull GenericMasterFieldType fieldType,
        boolean requiredFlag,
        boolean listVisibleFlag,
        boolean searchableFlag,
        @Min(0) int displayOrder,
        @Size(max = 50) String unit,
        @Size(max = 500) String placeholder,
        @Size(max = 1000) String validationPattern,
        @Size(max = 10000) String optionsJson
) {
}
