package com.project.backend.features.admin.business.dto;

import java.math.BigDecimal;

import com.project.backend.features.employee.enums.DormitoryType;

public record DormitoryFeeSettingResponse(
        Long id,
        DormitoryType dormitoryType,
        BigDecimal dailyAmount,
        boolean activeFlag
) {
}
