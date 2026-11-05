package com.project.backend.features.system.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SystemSettingDefinitionUpdateRequest(
        @NotBlank @Size(max = 200) String largeCategoryName,
        @NotBlank @Size(max = 200) String middleCategoryName,
        @NotBlank @Size(max = 200) String settingName,
        @Size(max = 1000) String description,
        @NotNull String valueType,
        boolean requiredFlag,
        @Size(max = 1000) String validationPattern,
        @Size(max = 500) String placeholder,
        int displayOrder,
        boolean sensitiveFlag,
        boolean activeFlag,
        @Size(max = 10000) String settingValue
) {
}
