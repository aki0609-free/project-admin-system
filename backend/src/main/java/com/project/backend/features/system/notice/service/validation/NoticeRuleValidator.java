package com.project.backend.features.system.notice.service.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.scheduling.support.CronExpression;

import com.project.backend.features.system.notice.dto.NoticeRuleSaveRequest;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.enums.NoticeDateType;
import com.project.backend.features.system.notice.enums.NoticeTargetDateSourceType;
import com.project.backend.features.system.notice.exception.NoticeRuleConflictException;
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
            NoticeRule entity,
            NoticeRuleSaveRequest request
    ) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "更新対象NoticeRuleは必須です。"
            );
        }

        validate(
                request,
                entity.getId()
        );

        if (!entity.getRuleCode().equals(
                request.ruleCode())) {
            throw new NoticeRuleConflictException(
                    "作成後のruleCodeは変更できません。 current="
                            + entity.getRuleCode()
                            + ", requested="
                            + request.ruleCode()
            );
        }
    }

    private void validate(
            NoticeRuleSaveRequest request,
            Long id
    ) {
        validateRequired(request);
        validateIdentifiers(request);
        validateDateSetting(request);
        NoticeSqlSafety.validateWhereClause(
                request.whereClause()
        );
        validateCronExpression(request.cronExpression());
        validateDuplicate(request, id);
    }

    private void validateRequired(
            NoticeRuleSaveRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("リクエストが不正です。");
        }

        requireText(request.ruleCode(), "ruleCode");
        requireText(request.ruleName(), "ruleName");
        requireText(request.targetTableName(), "targetTableName");
        requireText(request.targetKeyColumnName(), "targetKeyColumnName");
        requireText(request.noticeTitleTemplate(), "noticeTitleTemplate");
        requireText(request.noticeBodyTemplate(), "noticeBodyTemplate");

        NoticeTargetDateSourceType sourceType =
                request.targetDateSourceType() != null
                        ? request.targetDateSourceType()
                        : NoticeTargetDateSourceType.DATE_COLUMN;

        if (sourceType
                == NoticeTargetDateSourceType.DATE_COLUMN) {
            requireText(
                    request.targetDateColumnName(),
                    "targetDateColumnName"
            );
        } else {
            requireText(
                    request.targetDayTypeColumnName(),
                    "targetDayTypeColumnName"
            );
            requireText(
                    request.targetDayValueColumnName(),
                    "targetDayValueColumnName"
            );
        }
    }

    private void validateIdentifiers(
            NoticeRuleSaveRequest request
    ) {
        validateIdentifier(request.ruleCode(), "ruleCode");
        validateIdentifier(request.targetTableName(), "targetTableName");
        validateIdentifier(request.targetKeyColumnName(), "targetKeyColumnName");

        if (StringUtils.hasText(
                request.targetDateColumnName())) {
            validateIdentifier(
                    request.targetDateColumnName(),
                    "targetDateColumnName"
            );
        }

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
            throw new IllegalArgumentException("BEFORE_DAYS の場合 daysBefore は必須です。");
        }

        if (
                dateType == NoticeDateType.AFTER_DAYS
                        && request.daysBefore() == null
        ) {
            throw new IllegalArgumentException("AFTER_DAYS の場合 daysBefore は必須です。");
        }

        if (
                request.daysBefore() != null
                        && (request.daysBefore() < 0
                        || request.daysBefore() > 3650)
        ) {
            throw new IllegalArgumentException(
                    "daysBefore は0以上3650以下で指定してください。"
            );
        }

        if (dateType == NoticeDateType.DAY_OF_MONTH) {
            if (request.dayOfMonth() == null) {
                throw new IllegalArgumentException("DAY_OF_MONTH の場合 dayOfMonth は必須です。");
            }

            if (
                    request.dayOfMonth() < 1
                            || request.dayOfMonth() > 31
            ) {
                throw new IllegalArgumentException("dayOfMonth は 1〜31 で指定してください。");
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
            throw new IllegalArgumentException(
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
            throw new IllegalArgumentException(label + " は必須です。");
        }
    }

    private void validateIdentifier(
            String value,
            String label
    ) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(label + " は必須です。");
        }

        if (!SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    label + " に使用できない文字が含まれています。 value=" + value
            );
        }
    }

    private void validateCronExpression(
            String cronExpression
    ) {
        if (!StringUtils.hasText(cronExpression)) {
            return;
        }

        if (!CronExpression.isValidExpression(
                cronExpression.trim())) {
            throw new IllegalArgumentException(
                    "cronExpressionの形式が不正です。"
            );
        }
    }
}
