package com.project.backend.features.dashboard.dto;

import java.time.LocalDate;

import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.dashboard.enums.NoticeSourceType;

public record NoticeResponse(
        Long id,
        String title,
        LocalDate start,
        LocalDate end,
        String type,
        String color,
        NoticeContentFormat contentFormat,
        String content,
        NoticeSourceType sourceType,
        String sourceRuleCode,
        boolean pinnedFlag,
        boolean activeFlag
) {
}