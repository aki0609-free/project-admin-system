package com.project.backend.features.admin.business.dto;

public record ExternalSupportLinkSettingResponse(
        String incidentReportUrl,
        String manualUrl
) {
}
