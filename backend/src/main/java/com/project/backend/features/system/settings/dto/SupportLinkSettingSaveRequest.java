package com.project.backend.features.system.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportLinkSettingSaveRequest(
        @NotBlank @Size(max = 2048) String incidentReportUrl,
        @NotBlank @Size(max = 2048) String manualUrl
) {
}
