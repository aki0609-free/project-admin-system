package com.project.backend.features.master.allowance.dto;

public record AllowanceListItemResponse(
        Long id,
        String allowanceCode,
        String allowanceName,
        String allowanceType,
        String calculationType,
        String allowanceUnit,
        String detailViewType,
        String ruleName,
        Integer defaultAmount,
        Boolean allowManualInput,
        Integer minAmount,
        Integer maxAmount,
        Boolean taxable,
        Boolean showOnDailyStatement,
        Boolean showOnMonthlyStatement,
        Integer displayOrder,
        Boolean enabled,
        String note
) {
}