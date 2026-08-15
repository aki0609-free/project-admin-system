package com.project.backend.features.admin.business.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExternalSupportLinkSettingSaveRequest(
        @NotBlank @Size(max = 2048) String incidentReportUrl,
        @NotBlank @Size(max = 2048) String manualUrl
) {
}
