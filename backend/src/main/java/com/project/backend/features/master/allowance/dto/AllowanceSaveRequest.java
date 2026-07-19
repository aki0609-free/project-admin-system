package com.project.backend.features.master.allowance.dto;

import com.project.backend.features.master.allowance.enums.AllowanceCalculationType;
import com.project.backend.features.master.allowance.enums.AllowanceDetailViewType;
import com.project.backend.features.master.allowance.enums.AllowanceType;
import com.project.backend.features.master.allowance.enums.AllowanceUnit;

import jakarta.validation.constraints.NotBlank;

public record AllowanceSaveRequest(
        @NotBlank String allowanceCode,
        @NotBlank String allowanceName,
        AllowanceType allowanceType,
        AllowanceCalculationType calculationType,
        AllowanceUnit allowanceUnit,
        AllowanceDetailViewType detailViewType,
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