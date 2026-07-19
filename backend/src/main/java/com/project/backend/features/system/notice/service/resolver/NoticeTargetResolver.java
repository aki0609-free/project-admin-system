package com.project.backend.features.system.notice.service.resolver;

import java.util.List;

import com.project.backend.features.system.notice.dto.NoticeTargetRow;
import com.project.backend.features.system.notice.entity.NoticeRule;

public interface NoticeTargetResolver {
    List<NoticeTargetRow> resolve(NoticeRule rule);
}