package com.project.backend.features.system.notice.service.resolver;

import java.util.List;

import org.springframework.stereotype.Component;

import com.project.backend.features.system.notice.dto.NoticeTargetRow;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.enums.NoticeTargetDateSourceType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoticeTargetResolverDispatcher {

    private final DateColumnNoticeTargetResolver dateColumnResolver;
    private final DayRuleNoticeTargetResolver dayRuleResolver;

    public List<NoticeTargetRow> resolve(NoticeRule rule) {
        NoticeTargetDateSourceType sourceType =
                rule.getTargetDateSourceType() == null
                        ? NoticeTargetDateSourceType.DATE_COLUMN
                        : rule.getTargetDateSourceType();

        return switch (sourceType) {
            case DATE_COLUMN -> dateColumnResolver.resolve(rule);
            case DAY_RULE -> dayRuleResolver.resolve(rule);
        };
    }
}