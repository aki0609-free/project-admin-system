package com.project.backend.features.dailyreport.dto;

import lombok.Builder;

@Builder
public record DailyReportMissingEmployeeResponse(
        Long employeeId,
        String employeeCode,
        String employeeName
) {
}