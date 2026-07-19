package com.project.backend.features.dashboard.mapper;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.project.backend.features.dashboard.dto.NoticeResponse;
import com.project.backend.features.dashboard.entity.Notice;
import com.project.backend.features.dashboard.enums.NoticeType;

@Component
public class NoticeMapper {

    public NoticeResponse toResponse(Notice notice) {
        String type = notice.getType() != null
                ? notice.getType().name()
                : NoticeType.INFO.name();

        String color = StringUtils.hasText(notice.getColor())
                ? notice.getColor()
                : "blue";

        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getStartDate(),
                notice.getEndDate(),
                type,
                color,
                notice.getContentFormat(),
                notice.getContent(),
                notice.getSourceType(),
                notice.getSourceRuleCode(),
                notice.isPinnedFlag(),
                notice.isActiveFlag()
        );
    }
}