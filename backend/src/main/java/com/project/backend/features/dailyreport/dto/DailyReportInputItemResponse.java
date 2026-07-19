package com.project.backend.features.dailyreport.dto;

import com.project.backend.features.dailyreport.enums.DailyReportInputItemType;
import com.project.backend.features.dailyreport.enums.DailyReportInputMode;

import lombok.Builder;

@Builder
public record DailyReportInputItemResponse(
        Long masterId,
        String code,
        String name,
        DailyReportInputItemType itemType,
        DailyReportInputMode inputMode,
        Integer amount,
        Boolean editable,
        Integer displayOrder
) {
}