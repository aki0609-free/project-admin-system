package com.project.backend.features.admin.business.dto;

import com.project.backend.common.dayrule.dto.DayRule;

public record BusinessClosingSettingResponse(
        Long id,
        String settingCode,
        DayRule closingDay,
        DayRule paymentDay,
        boolean activeFlag
) {
}
