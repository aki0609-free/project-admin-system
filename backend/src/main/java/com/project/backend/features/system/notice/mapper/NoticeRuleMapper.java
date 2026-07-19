package com.project.backend.features.system.notice.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.util.StringUtils;

import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.system.notice.dto.NoticeRuleResponse;
import com.project.backend.features.system.notice.dto.NoticeRuleSaveRequest;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.enums.NoticeDateType;
import com.project.backend.features.system.notice.enums.NoticeSeverity;
import com.project.backend.features.system.notice.enums.NoticeTargetDateSourceType;

@Mapper(componentModel = "spring")
public interface NoticeRuleMapper {

    NoticeRuleResponse toResponse(NoticeRule entity);

    List<NoticeRuleResponse> toResponseList(List<NoticeRule> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "targetDateSourceType", expression = "java(resolveTargetDateSourceType(request))")
    @Mapping(target = "noticeContentFormat", expression = "java(resolveNoticeContentFormat(request))")
    @Mapping(target = "noticeSeverity", expression = "java(resolveNoticeSeverity(request))")
    @Mapping(target = "dateType", expression = "java(resolveDateType(request))")
    @Mapping(target = "activeFlag", expression = "java(request.activeFlagOrDefault())")
    @Mapping(target = "ruleCode", expression = "java(normalizeBlank(request.ruleCode()))")
    @Mapping(target = "ruleName", expression = "java(normalizeBlank(request.ruleName()))")
    @Mapping(target = "targetTableName", expression = "java(normalizeBlank(request.targetTableName()))")
    @Mapping(target = "targetKeyColumnName", expression = "java(normalizeBlank(request.targetKeyColumnName()))")
    @Mapping(target = "targetDateColumnName", expression = "java(normalizeBlank(request.targetDateColumnName()))")
    @Mapping(target = "targetDayTypeColumnName", expression = "java(normalizeBlank(request.targetDayTypeColumnName()))")
    @Mapping(target = "targetDayValueColumnName", expression = "java(normalizeBlank(request.targetDayValueColumnName()))")
    @Mapping(target = "targetLabelColumnName", expression = "java(normalizeBlank(request.targetLabelColumnName()))")
    @Mapping(target = "whereClause", expression = "java(normalizeBlank(request.whereClause()))")
    @Mapping(target = "noticeTitleTemplate", expression = "java(normalizeBlank(request.noticeTitleTemplate()))")
    @Mapping(target = "noticeBodyTemplate", expression = "java(normalizeBlank(request.noticeBodyTemplate()))")
    @Mapping(target = "cronExpression", expression = "java(normalizeBlank(request.cronExpression()))")
    void applyRequest(
            NoticeRuleSaveRequest request,
            @MappingTarget NoticeRule entity
    );

    default NoticeTargetDateSourceType resolveTargetDateSourceType(
            NoticeRuleSaveRequest request
    ) {
        return request.targetDateSourceType() != null
                ? request.targetDateSourceType()
                : NoticeTargetDateSourceType.DATE_COLUMN;
    }

    default NoticeContentFormat resolveNoticeContentFormat(
            NoticeRuleSaveRequest request
    ) {
        return request.noticeContentFormat() != null
                ? request.noticeContentFormat()
                : NoticeContentFormat.PLAIN_TEXT;
    }

    default NoticeSeverity resolveNoticeSeverity(
            NoticeRuleSaveRequest request
    ) {
        return request.noticeSeverity() != null
                ? request.noticeSeverity()
                : NoticeSeverity.INFO;
    }

    default NoticeDateType resolveDateType(
            NoticeRuleSaveRequest request
    ) {
        return request.dateType() != null
                ? request.dateType()
                : NoticeDateType.BEFORE_DAYS;
    }

    default String normalizeBlank(String value) {
        return StringUtils.hasText(value)
                ? value.trim()
                : null;
    }
}