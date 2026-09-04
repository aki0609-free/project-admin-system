package com.project.backend.features.dailyreport.dto;

import java.math.BigDecimal;

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
        Integer calculatedAmount,
        Integer amount,
        Boolean manualOverride,
        String overrideReason,
        Boolean editable,
        Integer displayOrder,
        Boolean balanceTracked,
        String balanceUnit,
        Boolean advanceConsumptionAllowed,
        BigDecimal openingQuantity,
        BigDecimal accruedQuantity,
        BigDecimal consumedQuantity,
        BigDecimal remainingQuantity,
        BigDecimal quantity,
        BigDecimal remainingAfterQuantity
) {
}
