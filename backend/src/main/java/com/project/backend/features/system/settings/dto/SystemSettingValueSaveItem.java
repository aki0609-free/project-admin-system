package com.project.backend.features.system.settings.dto;

import jakarta.validation.constraints.NotNull;

public record SystemSettingValueSaveItem(
        @NotNull Long definitionId,
        String value
) {
}
