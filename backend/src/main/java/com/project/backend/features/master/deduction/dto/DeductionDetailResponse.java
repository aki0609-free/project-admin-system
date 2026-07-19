package com.project.backend.features.master.deduction.dto;

import java.util.List;
import java.util.Map;

public record DeductionDetailResponse(
        Long id,
        String deductionCode,
        String deductionName,
        String deductionType,
        String calculationType,
        String deductionUnit,
        String detailViewType,

        String ruleName,
        Integer defaultAmount,
        Boolean allowManualInput,
        Integer minAmount,
        Integer maxAmount,

        Boolean showOnDailyStatement,
        Boolean showOnMonthlyStatement,
        Boolean carryToMonthlySettlement,
        Integer displayOrder,
        Boolean enabled,
        String note,

        Map<String, List<BaseDeductionDetailResponse>> details
) {
}