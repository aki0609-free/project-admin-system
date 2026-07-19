package com.project.backend.features.system.notice.dto;

import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.system.notice.enums.NoticeDateType;
import com.project.backend.features.system.notice.enums.NoticeSeverity;
import com.project.backend.features.system.notice.enums.NoticeTargetDateSourceType;

public record NoticeRuleSaveRequest(
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
        Boolean activeFlag
) {
    public boolean activeFlagOrDefault() {
        return activeFlag == null || activeFlag;
    }
}