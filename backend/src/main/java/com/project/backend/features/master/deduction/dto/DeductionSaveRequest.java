package com.project.backend.features.master.deduction.dto;

import com.project.backend.features.master.deduction.enums.DeductionCalculationType;
import com.project.backend.features.master.deduction.enums.DeductionDetailViewType;
import com.project.backend.features.master.deduction.enums.DeductionType;
import com.project.backend.features.master.deduction.enums.DeductionUnit;

import jakarta.validation.constraints.NotBlank;

public record DeductionSaveRequest(
        @NotBlank String deductionCode,
        @NotBlank String deductionName,
        DeductionType deductionType,
        DeductionCalculationType calculationType,
        DeductionUnit deductionUnit,
        DeductionDetailViewType detailViewType,
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
        String note
) {
}