package com.project.backend.common.dayrule.dto;

import com.project.backend.common.dayrule.enums.DayRuleType;

import lombok.Builder;

@Builder
public record DayRuleResponse(
        DayRuleType type,
        Integer value,
        Integer monthOffset,
        String label
) {
}