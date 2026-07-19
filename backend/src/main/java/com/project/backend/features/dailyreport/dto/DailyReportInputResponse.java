package com.project.backend.features.dailyreport.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record DailyReportInputResponse(
        List<DailyReportInputItemResponse> allowances,
        List<DailyReportInputItemResponse> deductions
) {
}