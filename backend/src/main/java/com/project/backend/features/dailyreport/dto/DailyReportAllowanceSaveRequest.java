package com.project.backend.features.dailyreport.dto;

public record DailyReportAllowanceSaveRequest(
        Long allowanceMasterId,
        String allowanceCode,
        String allowanceName,
        Integer amount
) {
}