package com.project.backend.features.system.notice.dto;

import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.system.notice.enums.NoticeDateType;
import com.project.backend.features.system.notice.enums.NoticeSeverity;
import com.project.backend.features.system.notice.enums.NoticeTargetDateSourceType;
import lombok.Builder;

@Builder
public record NoticeRuleResponse(
        Long id,
        String ruleCode,
        String ruleName,
        String targetTableName,
        String targetKeyColumnName,
        NoticeTargetDateSourceType targetDateSourceType,
        String targetDateColumnName,
        String targetDayTypeColumnName,
        String targetDayValueColumnName,
        String targetLabelColumnName,
        String whereClause,
        String noticeTitleTemplate,
        String noticeBodyTemplate,
        NoticeContentFormat noticeContentFormat,
        NoticeSeverity noticeSeverity,
        NoticeDateType dateType,
        Integer daysBefore,
        Integer dayOfMonth,
        String cronExpression,
        boolean activeFlag
) {
}