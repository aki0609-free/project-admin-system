package com.project.backend.features.system.notice.dto;

import java.time.LocalDate;

import lombok.Builder;

@Builder
public record NoticeTargetRow(
        String targetKey,
        LocalDate targetDate,
        String targetLabel
) {
}