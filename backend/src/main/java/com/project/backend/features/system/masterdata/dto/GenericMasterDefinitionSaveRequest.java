package com.project.backend.features.system.masterdata.dto;

import java.util.List;

import com.project.backend.features.system.masterdata.entity.GenericMasterPlacement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GenericMasterDefinitionSaveRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$")
        @Size(max = 100)
        String masterCode,
        @NotBlank @Size(max = 200) String masterName,
        @NotBlank @Size(max = 100) String tabName,
        @NotNull GenericMasterPlacement placement,
        @Size(max = 1000) String description,
        @Min(0) int displayOrder,
        boolean activeFlag,
        @NotEmpty List<@Valid GenericMasterFieldRequest> fields
) {
}
