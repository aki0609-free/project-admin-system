package com.project.backend.features.system.settings.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record SystemSettingValueSaveRequest(
        @NotNull List<@Valid SystemSettingValueSaveItem> values
) {
}
