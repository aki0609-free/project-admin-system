package com.project.backend.features.system.notice.service.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.system.notice.dto.NoticeRuleSaveRequest;
import com.project.backend.features.system.notice.entity.NoticeRule;
import com.project.backend.features.system.notice.enums.NoticeDateType;
import com.project.backend.features.system.notice.enums.NoticeSeverity;
import com.project.backend.features.system.notice.enums.NoticeTargetDateSourceType;
import com.project.backend.features.system.notice.exception.NoticeRuleConflictException;
import com.project.backend.features.system.notice.repository.NoticeRuleRepository;

class NoticeRuleValidatorTest {

    private NoticeRuleValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NoticeRuleValidator(
                mock(NoticeRuleRepository.class)
        );
    }

    @Test
    void validateForCreate_shouldAcceptDayRuleColumnsWithoutDateColumn() {
        validator.validateForCreate(request(
                "CLOSING_NOTICE",
                NoticeTargetDateSourceType.DAY_RULE,
                null,
                null,
                "closing_day_type",
                "closing_day_value",
                "0 0 9 * * *"
        ));
    }

    @Test
    void validateForUpdate_shouldRejectRuleCodeChange() {
        NoticeRule entity = new NoticeRule();
        entity.setId(1L);
        entity.setRuleCode("CLOSING_NOTICE");

        assertThatThrownBy(() ->
                validator.validateForUpdate(
                        entity,
                        request(
                                "RENAMED_NOTICE",
                                NoticeTargetDateSourceType.DATE_COLUMN,
                                "closing_date",
                                null,
                                null,
                                null,
                                "0 0 9 * * *"
                        )
                ))
                .isInstanceOf(
                        NoticeRuleConflictException.class
                );
    }

    @Test
    void validateForCreate_shouldRejectUnsafeWhereClause() {
        assertThatThrownBy(() ->
                validator.validateForCreate(request(
                        "UNSAFE_NOTICE",
                        NoticeTargetDateSourceType.DATE_COLUMN,
                        "closing_date",
                        "enabled = 1; DELETE FROM users",
                        null,
                        null,
                        "0 0 9 * * *"
                )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("whereClause");
    }

    @Test
    void validateForCreate_shouldRejectInvalidCron() {
        assertThatThrownBy(() ->
                validator.validateForCreate(request(
                        "INVALID_CRON",
                        NoticeTargetDateSourceType.DATE_COLUMN,
                        "closing_date",
                        null,
                        null,
                        null,
                        "invalid"
                )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cronExpression");
    }

    private NoticeRuleSaveRequest request(
            String ruleCode,
            NoticeTargetDateSourceType sourceType,
            String dateColumn,
            String whereClause,
            String dayTypeColumn,
            String dayValueColumn,
            String cronExpression
    ) {
        return new NoticeRuleSaveRequest(
                ruleCode,
                "通知Rule",
                "customers",
                "id",
                sourceType,
                dateColumn,
                dayTypeColumn,
                dayValueColumn,
                "name",
                whereClause,
                "{label}への通知",
                "{date}",
                NoticeContentFormat.PLAIN_TEXT,
                NoticeSeverity.INFO,
                NoticeDateType.BEFORE_DAYS,
                3,
                null,
                cronExpression,
                true
        );
    }
}
