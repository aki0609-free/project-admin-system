package com.project.backend.features.system.notice.service.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.project.backend.features.system.notice.dto.NoticeRuleSaveRequest;
import com.project.backend.features.system.notice.enums.NoticeDateType;
import com.project.backend.features.system.notice.repository.NoticeRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NoticeRuleValidator {

    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("^[a-zA-Z0-9_]+$");

    private final NoticeRuleRepository repository;

    public void validateForCreate(
            NoticeRuleSaveRequest request
    ) {
        validate(
                request,
                null
        );
    }

    public void validateForUpdate(
            Long id,
            NoticeRuleSaveRequest request
    ) {
        validate(
                request,
                id
        );
    }

    private void validate(
            NoticeRuleSaveRequest request,
            Long id
    ) {
        validateRequired(request);
        validateIdentifiers(request);
        validateDateSetting(request);
        validateDuplicate(request, id);
    }

    private void validateRequired(
            NoticeRuleSaveRequest request
    ) {
        if (request == null) {
            throw new RuntimeException("リクエストが不正です。");
        }

        requireText(request.ruleCode(), "ruleCode");
        requireText(request.ruleName(), "ruleName");
        requireText(request.targetTableName(), "targetTableName");
        requireText(request.targetKeyColumnName(), "targetKeyColumnName");
        requireText(request.targetDateColumnName(), "targetDateColumnName");
        requireText(request.noticeTitleTemplate(), "noticeTitleTemplate");
        requireText(request.noticeBodyTemplate(), "noticeBodyTemplate");
    }

    private void validateIdentifiers(
            NoticeRuleSaveRequest request
    ) {
        validateIdentifier(request.targetTableName(), "targetTableName");
        validateIdentifier(request.targetKeyColumnName(), "targetKeyColumnName");
        validateIdentifier(request.targetDateColumnName(), "targetDateColumnName");

        if (StringUtils.hasText(request.targetLabelColumnName())) {
            validateIdentifier(
                    request.targetLabelColumnName(),
                    "targetLabelColumnName"
            );
        }

        if (StringUtils.hasText(request.targetDayTypeColumnName())) {
            validateIdentifier(
                    request.targetDayTypeColumnName(),
                    "targetDayTypeColumnName"
            );
        }

        if (StringUtils.hasText(request.targetDayValueColumnName())) {
            validateIdentifier(
                    request.targetDayValueColumnName(),
                    "targetDayValueColumnName"
            );
        }
    }

    private void validateDateSetting(
            NoticeRuleSaveRequest request
    ) {
        NoticeDateType dateType =
                request.dateType() != null
                        ? request.dateType()
                        : NoticeDateType.BEFORE_DAYS;

        if (
                dateType == NoticeDateType.BEFORE_DAYS
                        && request.daysBefore() == null
        ) {
            throw new RuntimeException("BEFORE_DAYS の場合 daysBefore は必須です。");
        }

        if (
                dateType == NoticeDateType.AFTER_DAYS
                        && request.daysBefore() == null
        ) {
            throw new RuntimeException("AFTER_DAYS の場合 daysBefore は必須です。");
        }

        if (
                request.daysBefore() != null
                        && request.daysBefore() < 0
        ) {
            throw new RuntimeException("daysBefore は0以上で指定してください。");
        }

        if (dateType == NoticeDateType.DAY_OF_MONTH) {
            if (request.dayOfMonth() == null) {
                throw new RuntimeException("DAY_OF_MONTH の場合 dayOfMonth は必須です。");
            }

            if (
                    request.dayOfMonth() < 1
                            || request.dayOfMonth() > 31
            ) {
                throw new RuntimeException("dayOfMonth は 1〜31 で指定してください。");
            }
        }
    }

    private void validateDuplicate(
            NoticeRuleSaveRequest request,
            Long id
    ) {
        boolean exists =
                id == null
                        ? repository.existsByRuleCodeAndDeletedAtIsNull(
                                request.ruleCode()
                        )
                        : repository.existsByRuleCodeAndIdNotAndDeletedAtIsNull(
                                request.ruleCode(),
                                id
                        );

        if (exists) {
            throw new RuntimeException(
                    "ruleCode が重複しています。 ruleCode="
                            + request.ruleCode()
            );
        }
    }

    private void requireText(
            String value,
            String label
    ) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException(label + " は必須です。");
        }
    }

    private void validateIdentifier(
            String value,
            String label
    ) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException(label + " は必須です。");
        }

        if (!SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new RuntimeException(
                    label + " に使用できない文字が含まれています。 value=" + value
            );
        }
    }
}