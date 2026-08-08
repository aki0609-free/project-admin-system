package com.project.backend.features.admin.business.dto;

import java.math.BigDecimal;

import com.project.backend.features.employee.enums.DormitoryType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record DormitoryFeeSettingSaveRequest(
        @NotNull DormitoryType dormitoryType,
        @NotNull @DecimalMin("0.00") BigDecimal dailyAmount,
        Boolean activeFlag
) {
}
