package com.project.backend.features.dailyreport.dto;

public record DailyReportDeductionSaveRequest(
        Long deductionMasterId,
        String deductionCode,
        String deductionName,
        Integer amount
) {
}