package com.project.backend.features.admin.business.dto;

public record AnnualReportBackupSettingResponse(
        Integer fiscalYearStartMonth,
        Integer graceDays,
        Boolean startupEnabled,
        Boolean activeFlag
) {
}
