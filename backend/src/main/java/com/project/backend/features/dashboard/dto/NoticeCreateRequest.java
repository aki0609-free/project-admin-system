package com.project.backend.features.dashboard.dto;

import java.time.LocalDate;

import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.dashboard.enums.NoticeType;

public record NoticeCreateRequest(
        String title,
        LocalDate start,
        LocalDate end,
        NoticeType type,
        String color,
        NoticeContentFormat contentFormat,
        String content,
        Boolean pinnedFlag,
        Boolean activeFlag
) {
}